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
import com.faforever.client.player.UserOfflineEvent;
import com.faforever.client.player.UserOnlineEvent;
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
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.UtilSSLSocketFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.task.CompletableTask.Priority.HIGH;
import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

@Lazy
@Service
@Slf4j
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
public class PircBotXChatService implements ChatService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SOCKET_TIMEOUT = 10000;
  @VisibleForTesting
  final ObjectProperty<ConnectionState> connectionState;
  private final Map<Class<? extends GenericEvent>, ArrayList<ChatEventListener>> eventListeners;
  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, Channel> channels;
  private final ObservableMap<String, ChatUser> chatUsersByName;
  private final SimpleIntegerProperty unreadMessagesCount;

  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final UserService userService;
  private final TaskService taskService;
  private final FafService fafService;
  private final I18n i18n;
  private final PircBotXFactory pircBotXFactory;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final EventBus eventBus;
  private final String ircHost;
  private final int ircPort;
  private final String defaultChannelName;
  private final int reconnectDelay;

  private Configuration configuration;
  private PircBotX pircBotX;
  private CountDownLatch identifiedLatch;
  private Task<Void> connectionTask;
  /**
   * A list of channels the server wants us to join.
   */
  private List<String> autoChannels;
  /**
   * Indicates whether the "auto channels" already have been joined. This is needed to not auto join them twice.
   */
  private boolean autoChannelsJoined;

  @Inject
  public PircBotXChatService(PreferencesService preferencesService, UserService userService, TaskService taskService,
                             FafService fafService, I18n i18n, PircBotXFactory pircBotXFactory,
                             ThreadPoolExecutor threadPoolExecutor, EventBus eventBus,
                             ClientProperties clientProperties, PlayerService playerService) {
    this.preferencesService = preferencesService;
    this.userService = userService;
    this.taskService = taskService;
    this.fafService = fafService;
    this.i18n = i18n;
    this.pircBotXFactory = pircBotXFactory;
    this.threadPoolExecutor = threadPoolExecutor;
    this.eventBus = eventBus;
    this.playerService = playerService;

    Irc irc = clientProperties.getIrc();
    this.ircHost = irc.getHost();
    this.ircPort = irc.getPort();
    this.defaultChannelName = irc.getDefaultChannel();
    this.reconnectDelay = irc.getReconnectDelay();

    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    eventListeners = new ConcurrentHashMap<>();
    channels = observableHashMap();
    chatUsersByName = observableHashMap();
    unreadMessagesCount = new SimpleIntegerProperty();
    identifiedLatch = new CountDownLatch(1);
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(SocialMessage.class, this::onSocialMessage);
    connectionState.addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
        case CONNECTING:
          onDisconnected();
          break;
      }
    });

    addEventListener(NoticeEvent.class, this::onNotice);
    addEventListener(ConnectEvent.class, event -> connectionState.set(ConnectionState.CONNECTED));
    addEventListener(DisconnectEvent.class, event -> connectionState.set(ConnectionState.DISCONNECTED));
    addEventListener(UserListEvent.class, event -> onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers())));
    addEventListener(JoinEvent.class, event -> onUserJoinedChannel(event.getChannel().getName(), getOrCreateChatUser(event.getUser())));
    addEventListener(PartEvent.class, event -> onChatUserLeftChannel(event.getChannel().getName(), event.getUser().getNick()));
    addEventListener(QuitEvent.class, event -> onChatUserQuit(event.getUser().getNick()));
    addEventListener(TopicEvent.class, event -> getOrCreateChannel(event.getChannel().getName()).setTopic(event.getTopic()));
    addEventListener(MessageEvent.class, this::onMessage);
    addEventListener(ActionEvent.class, this::onAction);
    addEventListener(PrivateMessageEvent.class, this::onPrivateMessage);
    addEventListener(MotdEvent.class, this::onMotd);
    addEventListener(OpEvent.class, event -> {
      User recipient = event.getRecipient();
      if (recipient != null) {
        onModeratorSet(event.getChannel().getName(), recipient.getNick());
      }
    });

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.userToColorProperty(),
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.store()
    );
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), (observable, oldValue, newValue) -> {
      synchronized (chatUsersByName) {
        switch (newValue) {
          case CUSTOM:
            chatUsersByName.values().stream()
                .filter(chatUser -> chatPrefs.getUserToColor().containsKey(chatUser.getUsername().toLowerCase(US)))
                .forEach(chatUser -> chatUser.setColor(chatPrefs.getUserToColor().get(chatUser.getUsername().toLowerCase(US))));
            break;

          case RANDOM:
            for (ChatUser chatUser : chatUsersByName.values()) {
              chatUser.setColor(ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode()));
            }
            break;

          default:
            for (ChatUser chatUser : chatUsersByName.values()) {
              chatUser.setColor(null);
            }
        }
      }
    });
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
    identifiedLatch.countDown();

    if (!autoChannelsJoined) {
      joinAutoChannels();
    } else {
      synchronized (channels) {
        logger.debug("Joining all channels: {}", channels);
        channels.keySet().forEach(this::joinChannel);
      }
    }
  }

  private void joinAutoChannels() {
    logger.debug("Joining auto channel: {}", autoChannels);
    if (autoChannels == null) {
      return;
    }
    autoChannels.forEach(this::joinChannel);
    autoChannelsJoined = true;
  }

  private void onDisconnected() {
    synchronized (channels) {
      channels.values().forEach(Channel::clearUsers);
    }
  }

  private <T extends GenericEvent> void addEventListener(Class<T> eventClass, ChatEventListener<T> listener) {
    if (!eventListeners.containsKey(eventClass)) {
      eventListeners.put(eventClass, new ArrayList<>());
    }
    eventListeners.get(eventClass).add(listener);
  }

  private void onChatUserList(String channelName, List<ChatUser> users) {
    getOrCreateChannel(channelName).addUsers(users);
  }

  private List<ChatUser> chatUsers(ImmutableSortedSet<User> users) {
    return users.stream().map(this::getOrCreateChatUser).collect(Collectors.toList());
  }

  private void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    getOrCreateChannel(channelName).addUser(chatUser);
    // This should actually be posted by the player service, but since the server doesn't yet tell us about users
    // leaving, have to rely on IRC for that. To keep things consistent (and avoid redundant events) we chose to rely
    // on IRC, for now. As soon as the server informs about leaving users, we'll rely on the server instead.
    if (defaultChannelName.equals(channelName)) {
      eventBus.post(new UserOnlineEvent(chatUser.getUsername()));
    }
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    getOrCreateChannel(channelName).removeUser(username);
    if (userService.getUsername().equalsIgnoreCase(username)) {
      channels.remove(channelName);
    }
    if (defaultChannelName.equals(channelName)) {
      eventBus.post(new UserOfflineEvent(username));
    }
  }

  private void onChatUserQuit(String username) {
    synchronized (channels) {
      channels.values().forEach(channel -> channel.removeUser(username));
    }
    synchronized (chatUsersByName) {
      chatUsersByName.remove(username);
    }
  }

  private void onModeratorSet(String channelName, String username) {
    getOrCreateChannel(channelName).setModerator(username);
  }

  private void init() {
    String username = userService.getUsername();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(String.valueOf(userService.getUserId()))
        .setRealName(username)
        .addServer(ircHost, ircPort)
        .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
        .setAutoSplitMessage(true)
        .setEncoding(UTF_8)
        .addListener(this::onEvent)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .setMessageDelay(0)
        .setAutoReconnectDelay(reconnectDelay)
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
      threadPoolExecutor.execute(this::joinAutoChannels);
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
      logger.warn("Action event without user: {}", event);
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
      logger.warn("Action event without user: {}", event);
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
      logger.warn("Private message without user: {}", event);
      return;
    }
    logger.debug("Received private message: {}", event);

    Player sourcePlayer = playerService.getPlayerForUsername(user.getNick());
    if (sourcePlayer != null && sourcePlayer.getSocialStatus() == SocialStatus.FOE && preferencesService.getPreferences().getChat().getHideFoeMessages()) {
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
            logger.info("Connecting to IRC at {}:{}", server.getHostname(), server.getPort());
            pircBotX.startBot();
          } catch (IOException | IrcException | RuntimeException e) {
            connectionState.set(ConnectionState.DISCONNECTED);
          }
        }
        return null;
      }
    };
    threadPoolExecutor.execute(connectionTask);
  }

  @Override
  public void disconnect() {
    logger.info("Disconnecting from IRC");
    if (connectionTask != null) {
      connectionTask.cancel(false);
    }
    if (pircBotX.isConnected()) {
      pircBotX.stopBotReconnect();
      pircBotX.sendIRC().quitServer();
      channels.clear();
    }
    identifiedLatch = new CountDownLatch(1);
    autoChannelsJoined = false;
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
  public ChatUser getOrCreateChatUser(String username) {
    synchronized (chatUsersByName) {
      String lowerUsername = username.toLowerCase(US);
      if (!chatUsersByName.containsKey(lowerUsername)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;
        if (chatPrefs.getChatColorMode() == CUSTOM && chatPrefs.getUserToColor().containsKey(lowerUsername)) {
          color = chatPrefs.getUserToColor().get(lowerUsername);
        } else if (chatPrefs.getChatColorMode() == RANDOM) {
          color = ColorGeneratorUtil.generateRandomColor(lowerUsername.hashCode());
        }

        chatUsersByName.put(lowerUsername, new ChatUser(username, color));
      }
      return chatUsersByName.get(lowerUsername);
    }
  }

  @Override
  public void addUsersListener(String channelName, MapChangeListener<String, ChatUser> listener) {
    getOrCreateChannel(channelName).addUsersListeners(listener);
  }

  @Override
  public void addChatUsersByNameListener(MapChangeListener<String, ChatUser> listener) {
    synchronized (chatUsersByName) {
      JavaFxUtil.addListener(chatUsersByName, listener);
    }
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    synchronized (channels) {
      JavaFxUtil.addListener(channels, listener);
    }
  }

  @Override
  public void removeUsersListener(String channelName, MapChangeListener<String, ChatUser> listener) {
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
    logger.debug("Joining channel (waiting for identification): {}", channelName);
    noCatch(() -> identifiedLatch.await());
    logger.debug("Joining channel: {}", channelName);
    pircBotX.sendIRC().joinChannel(channelName);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return defaultChannelName.equals(channelName);
  }

  @Override
  @PreDestroy
  public void close() {
    // TODO clean up disconnect() and close()
    identifiedLatch.countDown();
    if (connectionTask != null) {
      connectionTask.cancel();
    }
    if (pircBotX != null) {
      pircBotX.sendIRC().quitServer();
    }
  }

  @Override
  public ChatUser getOrCreateChatUser(User user) {
    synchronized (chatUsersByName) {
      String username = user.getNick();
      String lowerUsername = username.toLowerCase(US);
      if (!chatUsersByName.containsKey(lowerUsername)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;

        if (chatPrefs.getChatColorMode() == CUSTOM && chatPrefs.getUserToColor().containsKey(lowerUsername)) {
          color = chatPrefs.getUserToColor().get(lowerUsername);
        } else if (chatPrefs.getChatColorMode() == RANDOM) {
          color = ColorGeneratorUtil.generateRandomColor(lowerUsername.hashCode());
        }

        chatUsersByName.put(lowerUsername, ChatUser.fromIrcUser(user, color));
      }
      return chatUsersByName.get(lowerUsername);
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

  interface ChatEventListener<T> {

    void onEvent(T event);
  }
}
