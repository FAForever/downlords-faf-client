package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
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
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
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

public class PircBotXChatService implements ChatService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SOCKET_TIMEOUT = 10000;
  private final Map<Class<? extends GenericEvent>, ArrayList<ChatEventListener>> eventListeners;
  /**
   * Maps channels by name.
   */
  private final ObservableMap<String, Channel> channels;
  private final ObservableMap<String, ChatUser> chatUsersByName;
  private final ObjectProperty<ConnectionState> connectionState;
  private final IntegerProperty unreadMessagesCount;
  @Resource
  PreferencesService preferencesService;
  @Resource
  UserService userService;
  @Resource
  PlayerService playerService;
  @Resource
  TaskService taskService;
  @Resource
  FafService fafService;
  @Resource
  I18n i18n;
  @Resource
  PircBotXFactory pircBotXFactory;
  @Resource
  NotificationService notificationService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  EventBus eventBus;
  @Value("${irc.host}")
  String ircHost;
  @Value("${irc.port}")
  int ircPort;
  @Value("${irc.defaultChannel}")
  String defaultChannelName;
  @Value("${irc.reconnectDelay}")
  int reconnectDelay;
  private Configuration configuration;
  private PircBotX pircBotX;
  private CountDownLatch identifiedLatch;
  private Task<Void> connectionTask;
  /**
   * A list of channels the server wants us to join.
   */
  private List<String> autoChannels;
  /**
   * Indicates whether the "auto channels" already have been joined. This is needed because we don't want to auto join channels after a reconnect that the user left before the reconnect.
   */
  private boolean autoChannelsJoined;

  public PircBotXChatService() {
    connectionState = new SimpleObjectProperty<>();
    eventListeners = new ConcurrentHashMap<>();
    channels = observableHashMap();
    chatUsersByName = observableHashMap();
    unreadMessagesCount = new SimpleIntegerProperty();
    identifiedLatch = new CountDownLatch(1);
  }

  @PostConstruct
  void postConstruct() {
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
    addEventListener(OpEvent.class, event -> {
      User recipient = event.getRecipient();
      if (recipient != null) {
        onModeratorSet(event.getChannel().getName(), recipient.getNick());
      }
    });

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        connect();
      } else {
        disconnect();
        autoChannelsJoined = false;
      }
    });

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatPrefs.userToColorProperty().addListener(
        (MapChangeListener<? super String, ? super Color>) change -> preferencesService.store()
    );
    chatPrefs.chatColorModeProperty().addListener((observable, oldValue, newValue) -> {
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

  private void onNotice(NoticeEvent event) {
    Configuration config = event.getBot().getConfiguration();
    UserHostmask hostmask = event.getUserHostmask();

    if (config.getNickservOnSuccess() != null && containsIgnoreCase(hostmask.getHostmask(), config.getNickservNick())) {
      String message = event.getMessage();
      if (containsIgnoreCase(message, config.getNickservOnSuccess()) || containsIgnoreCase(message, "registered under your account")) {
        onIdentified();
      } else if (message.contains("isn't registered")) {
        sendMessageInBackground("NickServ", format("register %s %s@users.faforever.com", getPassword(), userService.getUsername()));
      }
    }
  }

  private void onIdentified() {
    identifiedLatch.countDown();

    if (!autoChannelsJoined) {
      joinAutoChannels();
    } else {
      synchronized (channels) {
        channels.keySet().forEach(this::joinChannel);
      }
    }
  }

  private void joinAutoChannels() {
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
    String username = chatUser.getUsername();
    getOrCreateChannel(channelName).addUser(chatUser);
    Player player = playerService.getPlayerForUsername(username);
    if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOnlineNotification.title", username),
              i18n.get("friend.nowOnlineNotification.action"),
              IdenticonUtil.createIdenticon(player.getId()),
              event -> eventBus.post(new InitiatePrivateChatEvent(username))
          ));
    }
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    getOrCreateChannel(channelName).removeUser(username);
    if (userService.getUsername().equalsIgnoreCase(username)) {
      channels.remove(channelName);
    }
  }

  private void onChatUserQuit(String username) {
    synchronized (channels) {
      channels.values().forEach(channel -> channel.removeUser(username));
    }
    synchronized (chatUsersByName) {
      chatUsersByName.remove(username);
    }
    Player player = playerService.getPlayerForUsername(username);
    if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOfflineNotification.title", username), "",
              IdenticonUtil.createIdenticon(player.getId())
          ));
    }
  }

  private void onModeratorSet(String channelName, String username) {
    getOrCreateChannel(channelName).setModerator(username);
  }

  private void init() {
    String username = userService.getUsername();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(String.valueOf(userService.getUid()))
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
    return Hashing.md5().hashString(userService.getPassword(), UTF_8).toString();
  }

  private void onSocialMessage(SocialMessage socialMessage) {
    if (!autoChannelsJoined) {
      this.autoChannels = new ArrayList<>(socialMessage.getChannels());
      autoChannels.add(0, defaultChannelName);
      joinAutoChannels();
    }
  }

  @SuppressWarnings("unchecked")
  private void onEvent(Event event) {
    if (!eventListeners.containsKey(event.getClass())) {
      return;
    }
    eventListeners.get(event.getClass()).forEach(listener -> listener.onEvent(event));
  }

  public void addOnMessageListener(Consumer<ChatMessage> listener) {
    addEventListener(MessageEvent.class, event -> {
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
      listener.accept(
          new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), false)
      );
    });
    addEventListener(ActionEvent.class, event -> {
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
      listener.accept(
          new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), true)
      );
    });
  }

  @Override
  public void addOnPrivateChatMessageListener(Consumer<ChatMessage> listener) {
    addEventListener(PrivateMessageEvent.class,
        event -> {
          User user = event.getUser();
          if (user == null) {
            logger.warn("Private message without user: {}", event);
            return;
          }
          logger.debug("Received private message: {}", event);
          listener.accept(
              new ChatMessage(user.getNick(), Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage())
          );
        }
    );
  }

  @Override
  public void connect() {
    init();

    connectionTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
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
  }

  @Override
  public CompletionStage<String> sendMessageInBackground(String target, String message) {
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
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
      chatUsersByName.addListener(listener);
    }
  }

  @Override
  public void addChannelsListener(MapChangeListener<String, Channel> listener) {
    synchronized (channels) {
      channels.addListener(listener);
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
  public CompletionStage<String> sendActionInBackground(String target, String action) {
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
        updateTitle(i18n.get("chat.sendActionTask.title"));

        pircBotX.sendIRC().action(target, action);
        return action;
      }
    }).getFuture();
  }

  @Override
  public void joinChannel(String channelName) {
    noCatch(() -> identifiedLatch.await());
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
    if (connectionTask != null) {
      Platform.runLater(connectionTask::cancel);
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
  public ObjectProperty<ConnectionState> connectionStateProperty() {
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
    synchronized (unreadMessagesCount) {
      unreadMessagesCount.set(unreadMessagesCount.get() + delta);
    }
  }

  @Override
  public ReadOnlyIntegerProperty unreadMessagesCount() {
    return unreadMessagesCount;
  }

  interface ChatEventListener<T> {

    void onEvent(T event);
  }
}
