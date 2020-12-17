package com.faforever.client.chat;

import com.faforever.client.FafClientApplication;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.player.UserOfflineEvent;
import com.faforever.client.player.event.CurrentPlayerInfo;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.ui.tray.event.UpdateApplicationBadgeEvent;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedSet;
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
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.UserLevel;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.MotdEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.OpEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.chat.ChatUserCategory.MODERATOR;
import static com.faforever.client.task.CompletableTask.Priority.HIGH;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableMap;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
public class PircBotXChatService implements ChatService, InitializingBean, DisposableBean {

  public static final int MAX_GAMES_FOR_NEWBIE_CHANNEL = 50;
  private static final List<UserLevel> MODERATOR_USER_LEVELS = Arrays.asList(UserLevel.OP, UserLevel.HALFOP, UserLevel.SUPEROP, UserLevel.OWNER);
  private static final int SOCKET_TIMEOUT = 10000;
  private static final String NEWBIE_CHANNEL_NAME = "#newbie";

  private final PreferencesService preferencesService;
  private final UserService userService;
  private final TaskService taskService;
  private final FafService fafService;
  private final I18n i18n;
  private final PircBotXFactory pircBotXFactory;
  private final ExecutorService executorService;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  /** Key is the result of {@link #mapKey(String, String)}. */
  private final ObservableMap<String, ChatChannelUser> chatChannelUsersByChannelAndName = observableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
  @VisibleForTesting
  ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  private final Map<Class<? extends GenericEvent>, ArrayList<ChatEventListener>> eventListeners = new ConcurrentHashMap<>();
  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, Channel> channels = observableHashMap();
  private final SimpleIntegerProperty unreadMessagesCount = new SimpleIntegerProperty();
  private String defaultChannelName;

  private Configuration configuration;
  private PircBotX pircBotX;
  /** Called when the IRC server has confirmed our identity. */
  private CompletableFuture<Void> identifiedFuture = new CompletableFuture<>();
  private Task<Void> connectionTask;
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

    addEventListener(NoticeEvent.class, this::onNotice);
    addEventListener(ConnectEvent.class, event -> connectionState.set(ConnectionState.CONNECTED));
    addEventListener(DisconnectEvent.class, event -> connectionState.set(ConnectionState.DISCONNECTED));
    addEventListener(UserListEvent.class, event -> onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers(), event.getChannel().getName())));
    addEventListener(JoinEvent.class, this::onJoinEvent);
    addEventListener(PartEvent.class, event -> onChatUserLeftChannel(event.getChannel().getName(), event.getUser().getNick()));
    addEventListener(QuitEvent.class, event -> onChatUserQuit(event.getUser().getNick()));
    addEventListener(TopicEvent.class, event -> getOrCreateChannel(event.getChannel().getName()).setTopic(event.getTopic()));
    addEventListener(MessageEvent.class, this::onMessage);
    addEventListener(ActionEvent.class, this::onAction);
    addEventListener(PrivateMessageEvent.class, this::onPrivateMessage);
    addEventListener(MotdEvent.class, this::onMotd);
    addEventListener(OpEvent.class, this::onOp);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.store()
    );
    JavaFxUtil.addListener(chatPrefs.groupToColorProperty(),
        (MapChangeListener<? super ChatUserCategory, ? super Color>) change -> {
          preferencesService.store();
          updateUserColors(chatPrefs.getChatColorMode());
        }
    );
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> updateUserColors(newValue));
  }

  private void onOp(OpEvent event) {
    User recipient = event.getRecipient();
    if (recipient != null) {
      onModeratorSet(event.getChannel().getName(), recipient.getNick());
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

  private void updateUserColors(ChatColorMode chatColorMode) {
    if (chatColorMode == null) {
      chatColorMode = DEFAULT;
    }
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    synchronized (chatChannelUsersByChannelAndName) {
      switch (chatColorMode) {
        case RANDOM -> chatChannelUsersByChannelAndName.values()
            .forEach(chatUser -> chatUser.setColor(ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode())));

        default -> chatChannelUsersByChannelAndName.values()
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

  private void onJoinEvent(JoinEvent event) {
    User user = Objects.requireNonNull(event.getUser());
    log.debug("User joined channel: {}", user);
    onJoinEvent(event.getChannel().getName(), getOrCreateChatUser(user, event.getChannel().getName()));
  }

  private ChatChannelUser getOrCreateChatUser(User user, String channelName) {
    String username = user.getNick() != null ? user.getNick() : user.getLogin();

    boolean isModerator = user.getChannels().stream()
        .filter(channel -> channel.getName().equals(channelName))
        .flatMap(channel -> user.getUserLevels(channel).stream())
        .anyMatch(MODERATOR_USER_LEVELS::contains);

    return getOrCreateChatUser(username, channelName, isModerator);
  }

  private void onMotd(MotdEvent event) {
    sendIdentify(event.getBot().getConfiguration());
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

  private void onNotice(NoticeEvent event) {
    Configuration config = event.getBot().getConfiguration();
    UserHostmask hostmask = event.getUserHostmask();

    if (config.getNickservOnSuccess() != null && containsIgnoreCase(hostmask.getHostmask(), config.getNickservNick())) {
      String message = event.getMessage();
      if (containsIgnoreCase(message, config.getNickservOnSuccess()) || containsIgnoreCase(message, "registered under your account")) {
        onIdentified();
      } else if (message.contains("isn't registered")) {
        pircBotX.sendIRC().message(config.getNickservNick(), format("register %s %s@users.faforever.com", getPassword(), userService.getUsername()));
      } else if (message.contains(" registered")) {
        // We just registered and are now identified
        onIdentified();
      } else if (message.contains("choose a different nick")) {
        // The server didn't accept our IDENTIFY command, well then, let's send a private message to nickserv manually
        sendIdentify(config);
      }
    }
  }

  private void sendIdentify(Configuration config) {
    pircBotX.sendIRC().message(config.getNickservNick(), format("identify %s", getPassword()));
  }

  private void onIdentified() {
    identifiedFuture.thenAccept(aVoid -> {
      if (!autoChannelsJoined) {
        joinAutoChannels();
      } else {
        synchronized (channels) {
          log.debug("Joining all channels: {}", channels);
          channels.keySet().forEach(this::joinChannel);
        }
        joinSavedAutoChannels();
      }
    });
    identifiedFuture.complete(null);
  }

  @Subscribe
  public void onCurrentPlayerInfo(CurrentPlayerInfo currentPlayerInfo) {
    if (!newbieChannelJoined && currentPlayerInfo.getCurrentPlayer().getNumberOfGames() < MAX_GAMES_FOR_NEWBIE_CHANNEL) {
      joinChannel(NEWBIE_CHANNEL_NAME);
    }
    newbieChannelJoined = true;
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
    autoChannelsJoined = false;
    synchronized (channels) {
      channels.values().forEach(Channel::clearUsers);
    }
  }

  private <T extends GenericEvent> void addEventListener(Class<T> eventClass, ChatEventListener<T> listener) {
    eventListeners.computeIfAbsent(eventClass, aClass -> new ArrayList<>()).add(listener);
  }

  private void onChatUserList(String channelName, List<ChatChannelUser> users) {
    getOrCreateChannel(channelName).addUsers(users);
  }

  private List<ChatChannelUser> chatUsers(ImmutableSortedSet<User> users, String channel) {
    return users.stream().map(user -> getOrCreateChatUser(user, channel)).collect(Collectors.toList());
  }

  private void onJoinEvent(String channelName, ChatChannelUser chatUser) {
    getOrCreateChannel(channelName).addUser(chatUser);
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

  private void onChatUserQuit(String username) {
    synchronized (channels) {
      channels.values().forEach(channel -> onChatUserLeftChannel(channel.getName(), username));
    }
  }

  private void onModeratorSet(String channelName, String username) {
    getOrCreateChannel(channelName).addModerator(username);
  }

  private void init() {
    String username = userService.getUsername();

    Irc irc = clientProperties.getIrc();
    this.defaultChannelName = irc.getDefaultChannel();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(String.valueOf(userService.getUserId()))
        .setRealName(username)
        .addServer(irc.getHost(), irc.getPort())
        .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
        .setAutoSplitMessage(true)
        .setEncoding(UTF_8)
        .addListener(this::onEvent)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .setMessageDelay(new StaticDelay(0))
        .setAutoReconnectDelay(new StaticDelay(irc.getReconnectDelay()))
        .setNickservPassword(getPassword())
        .setAutoReconnect(true)
        .buildConfiguration();

    pircBotX = pircBotXFactory.createPircBotX(configuration);
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
      executorService.execute(this::joinAutoChannels);
    }
  }

  @SuppressWarnings("unchecked")
  private void onEvent(Event event) {
    if (!eventListeners.containsKey(event.getClass())) {
      return;
    }
    eventListeners.get(event.getClass()).forEach(listener -> listener.onEvent(event));
  }

  private void onAction(ActionEvent event) {
    User user = event.getUser();
    if (user == null) {
      log.warn("Action event without user: {}", event);
      return;
    }

    String source;
    org.pircbotx.Channel channel = event.getChannel();
    if (channel == null) {
      source = user.getNick();
    } else {
      source = channel.getName();
    }
    eventBus.post(new ChatMessageEvent(new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), true)));
  }

  private void onMessage(MessageEvent event) {
    User user = event.getUser();
    if (user == null) {
      log.warn("Action event without user: {}", event);
      return;
    }

    String source;
    org.pircbotx.Channel channel = event.getChannel();
    source = channel.getName();

    eventBus.post(new ChatMessageEvent(new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), false)));
  }

  private void onPrivateMessage(PrivateMessageEvent event) {
    User user = event.getUser();
    if (user == null) {
      log.warn("Private message without user: {}", event);
      return;
    }
    log.debug("Received private message: {}", event);

    ChatChannelUser sender = getOrCreateChatUser(user.getNick(), event.getUser().getNick(), false);
    if (sender != null
        && sender.getPlayer().isPresent()
        && sender.getPlayer().get().getSocialStatus() == SocialStatus.FOE
        && preferencesService.getPreferences().getChat().getHideFoeMessages()) {
      log.debug("Suppressing chat message from foe '{}'", user.getNick());
      return;
    }
    eventBus.post(new ChatMessageEvent(new ChatMessage(user.getNick(), Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage())));
  }

  @Override
  public void connect() {
    init();

    connectionTask = new Task<Void>() {
      @Override
      protected Void call() {
        while (!isCancelled()) {
          try {
            connectionState.set(ConnectionState.CONNECTING);
            Configuration.ServerEntry server = configuration.getServers().get(0);
            log.info("Connecting to IRC at {}:{}", server.getHostname(), server.getPort());
            pircBotX.startBot();
          } catch (IOException | IrcException | RuntimeException e) {
            connectionState.set(ConnectionState.DISCONNECTED);
          }
        }
        return null;
      }
    };
    executorService.execute(connectionTask);
  }

  @Override
  public void disconnect() {
    log.info("Disconnecting from IRC");
    if (connectionTask != null) {
      connectionTask.cancel(false);
    }
    if (pircBotX.isConnected()) {
      pircBotX.stopBotReconnect();
      pircBotX.sendIRC().quitServer();
      synchronized (channels) {
        channels.clear();
      }
    }
    identifiedFuture = new CompletableFuture<>();
    newbieChannelJoined = false;
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    eventBus.post(new ChatMessageEvent(new ChatMessage(target, Instant.now(), userService.getUsername(), message)));
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() {
        updateTitle(i18n.get("chat.sendMessageTask.title"));
        pircBotX.sendIRC().message(target, message);
        return message;
      }
    }).getFuture();
  }

  @Override
  public Channel getOrCreateChannel(String channelName) {
    synchronized (channels) {
      if (!channels.containsKey(channelName)) {
        channels.put(channelName, new Channel(channelName));
      }
      return channels.get(channelName);
    }
  }

  @Override
  public ChatChannelUser getOrCreateChatUser(String username, String channel, boolean isModerator) {
    synchronized (chatChannelUsersByChannelAndName) {
      String key = mapKey(username, channel);
      if (!chatChannelUsersByChannelAndName.containsKey(key)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Optional<Player> optionalPlayer = playerService.getPlayerForUsername(username);

        Color color = null;

        if (chatPrefs.getChatColorMode() == DEFAULT && chatPrefs.getUserToColor().containsKey(userToColorKey(username))) {
          color = chatPrefs.getUserToColor().get(userToColorKey(username));
        } else if (chatPrefs.getChatColorMode() == DEFAULT && isModerator && chatPrefs.getGroupToColor().containsKey(MODERATOR)) {
          color = chatPrefs.getGroupToColor().get(MODERATOR);
        } else if (chatPrefs.getChatColorMode() == DEFAULT && optionalPlayer.isPresent()) {
          ChatUserCategory chatUserCategory = optionalPlayer.map(player -> switch (player.getSocialStatus()) {
            case FRIEND -> ChatUserCategory.FRIEND;
            case FOE -> ChatUserCategory.FOE;
            default -> ChatUserCategory.OTHER;
          }).orElse(ChatUserCategory.CHAT_ONLY);
          color = chatPrefs.getGroupToColor().get(chatUserCategory);
        } else if (chatPrefs.getChatColorMode() == RANDOM) {
          color = ColorGeneratorUtil.generateRandomColor(userToColorKey(username).hashCode());
        }

        ChatChannelUser chatChannelUser = new ChatChannelUser(username, color, isModerator, optionalPlayer.orElse(null));
        chatChannelUsersByChannelAndName.put(key, chatChannelUser);
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
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    JavaFxUtil.addListener(channels, listener);
  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatChannelUser> listener) {
    getOrCreateChannel(channelName).removeUserListener(listener);
  }

  @Override
  public void leaveChannel(String channelName) {
    pircBotX.getUserChannelDao().getChannel(channelName).send().part();
  }

  @Override
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() {
        updateTitle(i18n.get("chat.sendActionTask.title"));

        pircBotX.sendIRC().action(target, action);
        return action;
      }
    }).getFuture();
  }

  @Override
  public void joinChannel(String channelName) {
    log.debug("Joining channel (waiting for identification): {}", channelName);
    identifiedFuture.thenAccept(aVoid -> {
      log.debug("Joining channel: {}", channelName);
      pircBotX.sendIRC().joinChannel(channelName);
    });
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
    identifiedFuture.cancel(false);
    if (connectionTask != null) {
      connectionTask.cancel();
    }
    if (pircBotX != null) {
      pircBotX.sendIRC().quitServer();
    }
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public void reconnect() {
    disconnect();
    connect();
  }

  @Override
  public void whois(String username) {
    pircBotX.sendIRC().whois(username);
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
  public ChatChannelUser getChatUser(String username, String channelName) {
    return Optional.ofNullable(chatChannelUsersByChannelAndName.get(mapKey(username, channelName)))
        .orElseThrow(() -> new IllegalArgumentException("Chat user '" + username + "' is unknown for channel '" + channelName + "'"));
  }

  @Override
  public String getDefaultChannelName() {
    return defaultChannelName;
  }

  private String mapKey(String username, String channelName) {
    return username + channelName;
  }

  interface ChatEventListener<T> {

    void onEvent(T event);
  }
}
