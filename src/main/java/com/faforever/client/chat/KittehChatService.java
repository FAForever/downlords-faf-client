package com.faforever.client.chat;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.lobby.IrcPasswordInfo;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.element.Actor;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.Mode;
import org.kitteh.irc.client.library.element.mode.ModeStatus.Action;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelModeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNamesUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionFailedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.event.user.WhoisEvent;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.synchronizedObservableMap;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class KittehChatService implements ChatService, InitializingBean, DisposableBean {

  private static final Logger ircLog = LoggerFactory.getLogger("faf-irc");

  public static final int MAX_GAMES_FOR_NEWBIE_CHANNEL = 50;
  private static final String NEWBIE_CHANNEL_NAME = "#newbie";
  private static final Set<Character> MODERATOR_PREFIXES = Set.of('~', '&', '@', '%');
  private final UserService userService;
  private final FafServerAccessor fafServerAccessor;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final ChatPrefs chatPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;


  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, ChatChannel> channels = synchronizedObservableMap(observableHashMap());
  private final InvalidationListener userColorsListener = observable -> updateUserColors();
  @VisibleForTesting
  ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  @VisibleForTesting
  String defaultChannelName;
  @VisibleForTesting
  DefaultClient client;
  private NickServ nickServ;
  private String username;
  private String password;
  /**
   * A list of channels the server wants us to join.
   */
  private List<String> autoChannels;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafServerAccessor.addEventListener(SocialInfo.class, this::onSocialMessage);
    connectionState.addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED, CONNECTING -> onDisconnected();
      }
    });

    JavaFxUtil.addListener(chatPrefs.groupToColorProperty(), new WeakInvalidationListener(userColorsListener));
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), new WeakInvalidationListener(userColorsListener));
  }

  private void updateUserColors() {
    channels.values().stream().map(ChatChannel::getUsers).flatMap(Collection::stream).forEach(this::populateColor);
  }

  @Override
  public boolean userExistsInAnyChannel(String username) {
    return client.getChannels().stream().map(channel -> channel.getUser(username)).anyMatch(Optional::isPresent);
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channelName) {
    Optional<Channel> channel = client.getChannel(channelName);

    return channel.flatMap(chan -> chan.getUser(username).map(user -> getOrCreateChatUser(user, chan)))
        .orElseGet(() -> {
          ChatChannel chatChannel = getOrCreateChannel(channelName);
          return chatChannel.getUser(username).orElseGet(() -> initializeUserForChannel(username, chatChannel));
        });
  }

  private ChatChannelUser getOrCreateChatUser(User user, Channel channel) {
    String username = user.getNick();
    String channelName = channel.getName();

    ChatChannel chatChannel = getOrCreateChannel(channelName);
    return chatChannel.getUser(username).orElseGet(() -> {
      ChatChannelUser chatChannelUser = initializeUserForChannel(username, chatChannel);

      boolean isModerator = channel.getUserModes(user)
          .stream()
          .flatMap(Collection::stream)
          .map(ChannelUserMode::getNickPrefix)
          .anyMatch(MODERATOR_PREFIXES::contains);
      chatChannelUser.setModerator(isModerator);
      return chatChannelUser;
    });
  }

  private ChatChannelUser initializeUserForChannel(String username, ChatChannel chatChannel) {
    ChatChannelUser chatChannelUser = new ChatChannelUser(username, chatChannel.getName());
    playerService.getPlayerByNameIfOnline(username).ifPresent(chatChannelUser::setPlayer);
    populateColor(chatChannelUser);
    chatChannel.addUser(chatChannelUser);
    return chatChannelUser;
  }

  @Subscribe
  public void onIrcPassword(IrcPasswordInfo event) {
    username = userService.getUsername();
    password = Hashing.md5().hashString(event.getPassword(), StandardCharsets.UTF_8).toString();
    if (connectionState.get() == ConnectionState.DISCONNECTED) {
      connect();
    }
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    disconnect();
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));
  }

  @Subscribe
  public void onLoggedInEvent(LoginSuccessEvent event) {
    if (userService.getOwnPlayer()
        .getRatings()
        .values()
        .stream()
        .mapToInt(LeaderboardStats::getNumberOfGames)
        .sum() < MAX_GAMES_FOR_NEWBIE_CHANNEL) {
      joinChannel(NEWBIE_CHANNEL_NAME);
    }
  }

  @Subscribe
  public void onPlayerOnline(PlayerOnlineEvent event) {
    PlayerBean player = event.player();

    channels.values()
        .stream()
        .map(channel -> channel.getUser(player.getUsername()))
        .flatMap(Optional::stream)
        .forEach(chatChannelUser -> fxApplicationThreadExecutor.execute(() -> {
          chatChannelUser.setPlayer(player);
          populateColor(chatChannelUser);
        }));
  }

  @Handler
  public void onConnect(ClientNegotiationCompleteEvent event) {
    connectionState.set(ConnectionState.CONNECTED);
    channels.keySet().forEach(this::joinChannel);
    joinSavedAutoChannels();
  }

  @Handler
  private void onJoinEvent(ChannelJoinEvent event) {
    User user = event.getActor();
    ircLog.debug("User joined channel: {}", user);
    getOrCreateChatUser(user, event.getChannel());
  }

  @Handler
  public void onChatUserList(ChannelNamesUpdatedEvent event) {
    Channel channel = event.getChannel();
    List<ChatChannelUser> users = channel.getUsers()
        .stream()
        .map(user -> getOrCreateChatUser(user, channel))
        .collect(Collectors.toList());
    getOrCreateChannel(channel.getName()).addUsers(users);
  }

  @Handler
  private void onPartEvent(ChannelPartEvent event) {
    User user = event.getActor();
    ircLog.debug("User left channel: {}", user);
    onChatUserLeftChannel(event.getChannel().getName(), user.getNick());
  }

  @Handler
  private void onChatUserQuit(UserQuitEvent event) {
    String username = event.getUser().getNick();
    channels.values().forEach(channel -> onChatUserLeftChannel(channel.getName(), username));
    playerService.removePlayerIfOnline(username);
  }

  @Handler
  private void onTopicChange(ChannelTopicEvent event) {
    String author = event.getNewTopic()
        .getSetter()
        .map(Actor::getName)
        .map(name -> name.replaceFirst("!.*", ""))
        .orElse("");
    String content = event.getNewTopic().getValue().orElse("");
    getOrCreateChannel(event.getChannel().getName()).setTopic(new ChannelTopic(author, content));
  }

  @Handler
  private void onChannelMessage(ChannelMessageEvent event) {
    User user = event.getActor();

    String channelName = event.getChannel().getName();

    getOrCreateChannel(channelName).addMessage(new ChatMessage(Instant.now(), user.getNick(), event.getMessage(), false));
  }

  @Handler
  private void onChannelCTCP(ChannelCtcpEvent event) {
    User user = event.getActor();

    String channelName = event.getChannel().getName();

    String message = event.getMessage().replace("ACTION", user.getNick());
    getOrCreateChannel(channelName).addMessage(new ChatMessage(Instant.now(), user.getNick(), message, true));
  }

  @Handler
  private void onChannelModeChanged(ChannelModeEvent event) {
    ChatChannel channel = getOrCreateChannel(event.getChannel().getName());
    event.getStatusList().getAll().forEach(channelModeStatus -> channelModeStatus.getParameter().ifPresent(username -> {
      Mode changedMode = channelModeStatus.getMode();
      Action modeAction = channelModeStatus.getAction();
      if (changedMode instanceof ChannelUserMode channelUserMode && MODERATOR_PREFIXES.contains(channelUserMode.getNickPrefix())) {
        ChatChannelUser chatChannelUser = getOrCreateChatUser(username, channel.getName());
        if (modeAction == Action.ADD) {
          chatChannelUser.setModerator(true);
        } else if (modeAction == Action.REMOVE) {
          chatChannelUser.setModerator(false);
        }
      }
    }));
  }

  @Handler
  private void onPrivateMessage(PrivateMessageEvent event) {
    User user = event.getActor();
    ircLog.debug("Received private message: {}", event);

    ChatChannelUser sender = getOrCreateChatUser(user.getNick(), user.getNick());
    if (sender.getPlayer()
        .map(PlayerBean::getSocialStatus)
        .map(status -> status == SocialStatus.FOE)
        .orElse(false) && chatPrefs.isHideFoeMessages()) {
      ircLog.debug("Suppressing chat message from foe '{}'", user.getNick());
      return;
    }

    getOrCreateChannel(user.getNick()).addMessage(new ChatMessage(Instant.now(), user.getNick(), event.getMessage()));
  }

  @Handler
  private void onWhoIs(WhoisEvent event) {
    if (event.getWhoisData().getRealName().map(realName -> username.equals(realName)).orElse(false)) {
      nickServ.startAuthentication();
    }
  }

  @Handler
  private void onNotice(PrivateNoticeEvent event) {
    String message = event.getMessage();

    if (message.contains("isn't registered")) {
      client.sendMessage("NickServ", String.format("register %s %s@users.faforever.com", password, client.getNick()));
    } else if (message.contains("you are now recognized")) {
      client.sendMessage("NickServ", String.format("recover %s", username));
    } else if (message.contains("You have regained control")) {
      client.setNick(username);
    }
  }

  private void joinAutoChannels() {
    log.trace("Joining auto channels: {}", autoChannels);
    if (autoChannels == null) {
      return;
    }
    autoChannels.forEach(this::joinChannel);
  }

  private void joinSavedAutoChannels() {
    ObservableList<String> savedAutoChannels = chatPrefs.getAutoJoinChannels();
    if (savedAutoChannels == null) {
      return;
    }
    log.trace("Joining user's saved auto channels: {}", savedAutoChannels);
    savedAutoChannels.forEach(this::joinChannel);
  }

  private void onDisconnected() {
    channels.values().forEach(ChatChannel::clearUsers);
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    ChatChannelUser oldChatUser = getOrCreateChannel(channelName).removeUser(username);
    if (oldChatUser == null) {
      return;
    }
    ircLog.debug("User '{}' left channel: {}", username, channelName);
    if (client.getNick().equalsIgnoreCase(username)) {
      channels.remove(channelName);
    }
  }

  private void onMessage(String message) {
    message = message.replace(password, "*****");
    ircLog.debug(message);
  }

  @Handler
  private void onDisconnect(ClientConnectionEndedEvent event) {
    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @Handler
  private void onFailedConnect(ClientConnectionFailedEvent event) {
    connectionState.set(ConnectionState.DISCONNECTED);
    client.shutdown();
  }

  private void onSocialMessage(SocialInfo socialMessage) {
    this.autoChannels = new ArrayList<>(socialMessage.getChannels());
    autoChannels.remove(defaultChannelName);
    autoChannels.add(0, defaultChannelName);
    joinAutoChannels();
  }

  private void populateColor(ChatChannelUser chatChannelUser) {
    String lowercaseUsername = chatChannelUser.getUsername().toLowerCase(US);

    Color color;
    ObservableMap<ChatUserCategory, Color> groupToColor = chatPrefs.getGroupToColor();
    ObservableMap<String, Color> userToColor = chatPrefs.getUserToColor();

    if (chatPrefs.getChatColorMode() == RANDOM) {
      color = ColorGeneratorUtil.generateRandomColor(lowercaseUsername.hashCode());
    } else if (userToColor.containsKey(lowercaseUsername)) {
      color = userToColor.get(lowercaseUsername);
    } else {
      color = chatChannelUser.getCategories()
          .stream()
          .sorted()
          .map(groupToColor::get)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }
    chatChannelUser.setColor(color);
  }

  @Override
  public void connect() {
    Irc irc = clientProperties.getIrc();
    this.defaultChannelName = irc.getDefaultChannel();

    client = (DefaultClient) Client.builder()
        .user(String.valueOf(userService.getUserId()))
        .realName(username)
        .nick(username)
        .server()
        .host(irc.getHost())
        .port(irc.getPort(), SecurityType.SECURE)
        .secureTrustManagerFactory(new TrustEveryoneFactory())
        .then()
        .listeners()
        .input(this::onMessage)
        .output(this::onMessage)
        .then()
        .build();

    nickServ = NickServ.builder(client).account(username).password(password).build();

    client.getEventManager().registerEventListener(this);
    client.getActorTracker().setQueryChannelInformation(false);
    client.connect();
  }

  @Override
  public void disconnect() {
    log.info("Disconnecting from IRC");
    client.shutdown("Goodbye");
  }

  @Override
  public CompletableFuture<Void> sendMessageInBackground(ChatChannel chatChannel, String message) {
    return CompletableFuture.runAsync(() -> {
      client.sendMessage(chatChannel.getName(), message);
      chatChannel.addMessage(new ChatMessage(Instant.now(), client.getNick(), message));
    });
  }

  @Override
  public ChatChannel getOrCreateChannel(String channelName) {
    return channels.computeIfAbsent(channelName, ChatChannel::new);
  }

  public void addUsersListener(String channelName, ListChangeListener<ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, ChatChannel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  @Override
  public void removeChannelsListener(MapChangeListener<String, ChatChannel> listener) {
    JavaFxUtil.removeListener(channels, listener);
  }

  @Override
  public void leaveChannel(ChatChannel channel) {
    if (!channel.isPrivateChannel()) {
      client.removeChannel(channel.getName());
    } else {
      channels.remove(channel.getName());
    }
  }

  @Override
  public CompletableFuture<Void> sendActionInBackground(ChatChannel chatChannel, String action) {
    return CompletableFuture.runAsync(() -> {
      client.sendCtcpMessage(chatChannel.getName(), "ACTION " + action);
      chatChannel.addMessage(new ChatMessage(Instant.now(), client.getNick(), action, true));
    });
  }

  @Override
  public void joinChannel(String channelName) {
    log.debug("Joining channel: {}", channelName);
    client.addChannel(channelName);
  }

  @Override
  public void setChannelTopic(ChatChannel chatChannel, String text) {
    client.getChannel(chatChannel.getName())
        .orElseThrow(() -> new IllegalArgumentException(String.format("No channel with `%s` name", chatChannel.getName())))
        .setTopic(text);
  }

  @Override
  public boolean isDefaultChannel(ChatChannel chatChannel) {
    return defaultChannelName.equals(chatChannel.getName());
  }

  @Override
  public void destroy() {
    close();
  }

  public void close() {
    if (client != null) {
      client.shutdown();
    }
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public void reconnect() {
    if (client.isConnectionAlive()) {
      client.reconnect();
    } else {
      client.connect();
    }
  }

  @Override
  public void whois(String username) {
    client.sendRawLine("WHOIS " + username);
  }

  @Override
  public void incrementUnreadMessagesCount(int delta) {
    eventBus.post(UpdateApplicationBadgeEvent.ofDelta(delta));
  }

  @Override
  @Subscribe
  public void onInitiatePrivateChat(InitiatePrivateChatEvent event) {
    getOrCreateChannel(event.username());
  }
}
