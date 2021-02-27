package com.faforever.client.chat;

import com.faforever.client.FafClientApplication;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerOnlineEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.player.UserOfflineEvent;
import com.faforever.client.player.event.CurrentPlayerInfo;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.Hashing;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.defaults.DefaultClient;
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
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.ChatUserCategory.MODERATOR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableMap;

@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
public class KittehChatService implements ChatService, InitializingBean, DisposableBean {

  public static final int MAX_GAMES_FOR_NEWBIE_CHANNEL = 50;
  private static final String NEWBIE_CHANNEL_NAME = "#newbie";
  private static final Set<Character> MODERATOR_PREFIXES = Set.of('~', '&', '@', '%');
  private final ChatUserService chatUserService;
  private final PreferencesService preferencesService;
  private final UserService userService;
  private final FafService fafService;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, ChatChannel> channels = observableHashMap();
  /** Key is the result of {@link #mapKey(String, String)}. */
  private final ObservableMap<String, ChatChannelUser> chatChannelUsersByChannelAndName = observableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
  private final SimpleIntegerProperty unreadMessagesCount = new SimpleIntegerProperty();
  @VisibleForTesting
  ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  @VisibleForTesting
  String defaultChannelName;
  @VisibleForTesting
  DefaultClient client;
  private NickServ nickServ;
  /**
   * A list of channels the server wants us to join.
   */
  private List<String> autoChannels;
  /**
   * Indicates whether the "auto channels" already have been joined. This is needed because we don't want to auto join
   * channels after a reconnect that the user left before the reconnect.
   */
  private boolean autoChannelsJoined;
  private boolean newbieChannelJoined;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafService.addOnMessageListener(SocialMessage.class, this::onSocialMessage);
    connectionState.addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED, CONNECTING -> onDisconnected();
      }
    });

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.storeInBackground()
    );
    JavaFxUtil.addListener(chatPrefs.groupToColorProperty(),
        (MapChangeListener<? super ChatUserCategory, ? super Color>) change -> {
          preferencesService.storeInBackground();
          updateUserColors(chatPrefs.getChatColorMode());
        }
    );
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> updateUserColors(newValue));
  }

  private void updateUserColors(ChatColorMode chatColorMode) {
    if (chatColorMode == null) {
      chatColorMode = DEFAULT;
    }
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    synchronized (chatChannelUsersByChannelAndName) {
      if (chatColorMode == ChatColorMode.RANDOM) {
        chatChannelUsersByChannelAndName.values()
            .forEach(chatUser -> chatUser.setColor(ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode())));
      } else {
        chatChannelUsersByChannelAndName.values()
            .forEach(chatUser -> {
              if (chatPrefs.getUserToColor().containsKey(userToColorKey(chatUser.getUsername()))) {
                chatUser.setColor(chatPrefs.getUserToColor().get(userToColorKey(chatUser.getUsername())));
              } else {
                if (chatUser.isModerator() && chatPrefs.getGroupToColor().containsKey(MODERATOR)) {
                  chatUser.setColor(chatPrefs.getGroupToColor().get(MODERATOR));
                } else {
                  chatUser.setColor(chatUser.getSocialStatus()
                      .map(status -> chatPrefs.getGroupToColor().getOrDefault(groupToColorKey(status), null))
                      .orElse(null));
                }
              }
            });
      }
    }
  }

  @NotNull
  private String userToColorKey(String username) {
    return username.toLowerCase(US);
  }

  @NotNull
  private ChatUserCategory groupToColorKey(SocialStatus socialStatus) {
    return switch (socialStatus) {
      case FRIEND -> ChatUserCategory.FRIEND;
      case FOE -> ChatUserCategory.FOE;
      default -> ChatUserCategory.OTHER;
    };
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channelName) {
    Channel channel = client.getChannel(channelName).orElseThrow(() -> new IllegalArgumentException("Channel '" + channelName + "' is unknown"));
    User user = channel.getUser(username).orElseThrow(() -> new IllegalArgumentException("Chat user '" + username + "' is unknown for channel '" + channelName + "'"));

    return getOrCreateChatUser(user, channel);
  }

  private ChatChannelUser getOrCreateChatUser(User user, Channel channel) {
    String username = user.getNick();

    boolean isModerator = channel.getUserModes(user).stream().flatMap(Collection::stream)
        .map(ChannelUserMode::getNickPrefix)
        .anyMatch(MODERATOR_PREFIXES::contains);

    return getOrCreateChatUser(username, channel.getName(), isModerator);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    connect();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    disconnect();
    eventBus.post(UpdateApplicationBadgeEvent.ofNewValue(0));
  }

  @Subscribe
  public void onCurrentPlayerInfo(CurrentPlayerInfo currentPlayerInfo) {
    if (!newbieChannelJoined && currentPlayerInfo.getCurrentPlayer().getNumberOfGames() < MAX_GAMES_FOR_NEWBIE_CHANNEL) {
      joinChannel(NEWBIE_CHANNEL_NAME);
    }
    newbieChannelJoined = true;
  }

  @Subscribe
  public void onPlayerOnline(PlayerOnlineEvent event) {
    Player player = event.getPlayer();

    synchronized (channels) {
      channels.values().parallelStream()
          .map(channel -> chatChannelUsersByChannelAndName.get(mapKey(player.getUsername(), channel.getName())))
          .filter(Objects::nonNull)
          .forEach(chatChannelUser -> {
            chatUserService.associatePlayerToChatUser(chatChannelUser, player);
            eventBus.post(new ChatUserCategoryChangeEvent(chatChannelUser));
          });
    }
  }

  @Handler
  public void onConnect(ClientNegotiationCompleteEvent event) {
    connectionState.set(ConnectionState.CONNECTED);
  }

  @Handler
  private void onJoinEvent(ChannelJoinEvent event) {
    User user = event.getActor();
    log.debug("User joined channel: {}", user);
    addUserToChannel(event.getChannel().getName(), getOrCreateChatUser(user, event.getChannel()));
  }

  @Handler
  public void onChatUserList(ChannelNamesUpdatedEvent event) {
    Channel channel = event.getChannel();
    List<ChatChannelUser> users = channel.getUsers().stream().map(user -> getOrCreateChatUser(user, channel)).collect(Collectors.toList());
    getOrCreateChannel(channel.getName()).addUsers(users);
  }

  @Handler
  private void onPartEvent(ChannelPartEvent event) {
    User user = event.getActor();
    log.debug("User joined channel: {}", user);
    onChatUserLeftChannel(event.getChannel().getName(), user.getNick());
  }

  @Handler
  private void onChatUserQuit(UserQuitEvent event) {
    User user = event.getUser();
    synchronized (channels) {
      channels.values().forEach(channel -> onChatUserLeftChannel(channel.getName(), user.getNick()));
    }
  }

  @Handler
  private void onTopicChange(ChannelTopicEvent event) {
    Channel channel = event.getChannel();
    getOrCreateChannel(channel.getName()).setTopic(event.getNewTopic().getValue().orElse(""));
  }

  @Handler
  private void onChannelMessage(ChannelMessageEvent event) {
    User user = event.getActor();

    String source = event.getChannel().getName();

    eventBus.post(new ChatMessageEvent(new ChatMessage(source, Instant.now(), user.getNick(), event.getMessage(), false)));
  }

  @Handler
  private void onChannelCTCP(ChannelCtcpEvent event) {
    User user = event.getActor();

    Channel channel = event.getChannel();
    String source = channel.getName();

    eventBus.post(new ChatMessageEvent(new ChatMessage(source, Instant.ofEpochMilli(user.getCreationTime()), user.getNick(), event.getMessage().replace("ACTION", user.getNick()), true)));
  }

  @Handler
  private void onChannelModeChanged(ChannelModeEvent event) {
    ChatChannel channel = getOrCreateChannel(event.getChannel().getName());
    event.getStatusList().getAll().forEach(channelModeStatus ->
        channelModeStatus.getParameter().ifPresent(username -> {
          Mode changedMode = channelModeStatus.getMode();
          Action modeAction = channelModeStatus.getAction();
          if (changedMode instanceof ChannelUserMode) {
            if (MODERATOR_PREFIXES.contains(((ChannelUserMode) changedMode).getNickPrefix())) {
              ChatChannelUser chatChannelUser = getOrCreateChatUser(username, channel.getName(), false);
              if (modeAction == Action.ADD) {
                chatChannelUser.setModerator(true);
              } else if (modeAction == Action.REMOVE) {
                chatChannelUser.setModerator(false);
              }
              eventBus.post(new ChatUserCategoryChangeEvent(chatChannelUser));
            }
          }
        }));
  }

  @Handler
  private void onPrivateMessage(PrivateMessageEvent event) {
    User user = event.getActor();
    log.debug("Received private message: {}", event);

    ChatChannelUser sender = getOrCreateChatUser(user.getNick(), user.getNick(), false);
    if (sender.getPlayer().map(Player::getSocialStatus).filter(status -> status == SocialStatus.FOE).isPresent()
        && preferencesService.getPreferences().getChat().getHideFoeMessages()) {
      log.debug("Suppressing chat message from foe '{}'", user.getNick());
      return;
    }
    eventBus.post(new ChatMessageEvent(new ChatMessage(user.getNick(), Instant.ofEpochMilli(user.getCreationTime()), user.getNick(), event.getMessage())));
  }

  @Handler
  private void onNotice(PrivateNoticeEvent event) {
    String message = event.getMessage();

    if (message.contains("choose a different nick")) {
      nickServ.startAuthentication();
    } else if (message.contains("isn't registered")) {
      client.sendMessage("NickServ", String.format("register %s %s@users.faforever.com", getPassword(), client.getNick()));
    }
  }

  private void joinAutoChannels() {
    log.debug("Joining auto channel: {}", autoChannels);
    if (autoChannels == null) {
      return;
    }
    autoChannels.forEach(this::joinChannel);
    autoChannelsJoined = true;
  }

  private void joinSavedAutoChannels() {
    ObservableList<String> savedAutoChannels = preferencesService.getPreferences().getChat().getAutoJoinChannels();
    if (savedAutoChannels == null) {
      return;
    }
    log.debug("Joining user's saved auto channel: {}", savedAutoChannels);
    savedAutoChannels.forEach(this::joinChannel);
  }

  private void onDisconnected() {
    synchronized (channels) {
      channels.values().forEach(ChatChannel::clearUsers);
      channels.clear();
    }
    synchronized (chatChannelUsersByChannelAndName) {
      chatChannelUsersByChannelAndName.clear();
    }
    newbieChannelJoined = false;
    autoChannelsJoined = false;
  }

  private void addUserToChannel(String channelName, ChatChannelUser chatUser) {
    getOrCreateChannel(channelName).addUser(chatUser);
    if (chatUser.isModerator()) {
      onModeratorSet(channelName, chatUser.getUsername());
    }
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    if (getOrCreateChannel(channelName).removeUser(username) == null) {
      return;
    }
    log.debug("User '{}' left channel: {}", username, channelName);
    if (userService.getUsername().equalsIgnoreCase(username)) {
      synchronized (channels) {
        channels.remove(channelName);
      }
    }
    synchronized (chatChannelUsersByChannelAndName) {
      chatChannelUsersByChannelAndName.remove(mapKey(username, channelName));
    }
    // The server doesn't yet tell us when a user goes offline, so we have to rely on the user leaving IRC.
    if (defaultChannelName.equals(channelName)) {
      eventBus.post(new UserOfflineEvent(username));
    }
  }

  private void onMessage(String message) {
    log.debug(message);
  }

  @Handler
  private void onDisconnect(ClientConnectionEndedEvent event) {
    connectionState.set(ConnectionState.DISCONNECTED);
  }

  @NotNull
  private String getPassword() {
    return Hashing.md5().hashString(Hashing.sha256().hashString(userService.getPassword(), UTF_8).toString(), UTF_8).toString();
  }

  private void onSocialMessage(SocialMessage socialMessage) {
    if (!autoChannelsJoined && socialMessage.getChannels() != null) {
      this.autoChannels = new ArrayList<>(socialMessage.getChannels());
      autoChannels.remove(defaultChannelName);
      autoChannels.add(0, defaultChannelName);
      joinAutoChannels();
      joinSavedAutoChannels();
    }
  }

  @Override
  public void connect() {
    String username = userService.getUsername();

    Irc irc = clientProperties.getIrc();
    this.defaultChannelName = irc.getDefaultChannel();

    client = (DefaultClient) Client.builder()
        .user(username)
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

    nickServ = NickServ.builder(client).account(username).password(getPassword()).build();

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
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    eventBus.post(new ChatMessageEvent(new ChatMessage(target, Instant.now(), userService.getUsername(), message)));
    return CompletableFuture.supplyAsync(() -> {
      client.sendMessage(target, message);
      return message;
    });
  }

  @Override
  public ChatChannel getOrCreateChannel(String channelName) {
    synchronized (channels) {
      if (!channels.containsKey(channelName)) {
        channels.put(channelName, new ChatChannel(channelName));
      }
      return channels.get(channelName);
    }
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    synchronized (chatChannelUsersByChannelAndName) {
      String key = mapKey(username, channel);
      if (!chatChannelUsersByChannelAndName.containsKey(key)) {
        Optional<Player> optionalPlayer = playerService.getPlayerForUsername(username);

        ChatChannelUser chatChannelUser = new ChatChannelUser(username, isModerator);
        chatChannelUsersByChannelAndName.put(key, chatChannelUser);
        chatUserService.associatePlayerToChatUser(chatChannelUser, optionalPlayer.orElse(null));
      }
      return chatChannelUsersByChannelAndName.get(key);
    }
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatChannelUser> listener) {
    synchronized (chatChannelUsersByChannelAndName) {
      JavaFxUtil.addListener(chatChannelUsersByChannelAndName, listener);
    }
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, ChatChannel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).removeUserListener(listener);
  }

  @Override
  public void leaveChannel(String channelName) {
    client.removeChannel(channelName);
  }

  @Override
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return CompletableFuture.supplyAsync(() -> {
      client.sendCtcpMessage(target, "ACTION " + action);
      return action;
    });
  }

  @Override
  public void joinChannel(String channelName) {
    log.debug("Joining channel: {}", channelName);
    client.addChannel(channelName);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return defaultChannelName.equals(channelName);
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
  public void reconnect() {
    Set<String> currentChannels = channels.keySet();
    client.reconnect();
    currentChannels.forEach(this::joinChannel);
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
  public ReadOnlyIntegerProperty unreadMessagesCount() {
    return unreadMessagesCount;
  }

  @Override
  public String getDefaultChannelName() {
    return defaultChannelName;
  }

  private void onModeratorSet(String channelName, String username) {
    getOrCreateChatUser(username, channelName, true).setModerator(true);
  }

  private String mapKey(String username, String channelName) {
    return username + channelName;
  }
}
