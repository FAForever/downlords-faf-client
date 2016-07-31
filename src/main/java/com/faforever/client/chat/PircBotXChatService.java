package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.collect.ImmutableSortedSet;
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
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.OpEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.UserListEvent;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static javafx.collections.FXCollections.observableHashMap;

public class PircBotXChatService implements ChatService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SOCKET_TIMEOUT = 10000;
  private final Map<Class<? extends Event>, ArrayList<ChatEventListener>> eventListeners;
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
  @Value("${irc.host}")
  String ircHost;
  @Value("${irc.port}")
  int ircPort;
  @Value("${irc.defaultChannel}")
  String defaultChannelName;
  @Value("${irc.reconnectDelay}")
  int reconnectDelay;
  private Consumer<String> onOpenPrivateChatListener;
  private Configuration configuration;
  private PircBotX pircBotX;
  private CountDownLatch chatConnectedLatch;
  private Task<Void> connectionTask;
  public PircBotXChatService() {
    connectionState = new SimpleObjectProperty<>();
    eventListeners = new ConcurrentHashMap<>();
    channels = observableHashMap();
    chatUsersByName = observableHashMap();
    unreadMessagesCount = new SimpleIntegerProperty();
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
        case CONNECTED:
          onConnected();
          break;
      }
    });

    addEventListener(ConnectEvent.class, event -> connectionState.set(ConnectionState.CONNECTED));
    addEventListener(DisconnectEvent.class, event -> connectionState.set(ConnectionState.DISCONNECTED));
    addEventListener(UserListEvent.class, event -> onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers())));
    addEventListener(JoinEvent.class, event -> onUserJoinedChannel(event.getChannel().getName(), createOrGetChatUser(event.getUser())));
    addEventListener(PartEvent.class, event -> onChatUserLeftChannel(event.getChannel().getName(), event.getUser().getNick()));
    addEventListener(QuitEvent.class, event -> onChatUserQuit(event.getUser().getNick()));
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

  private void onDisconnected() {
    synchronized (channels) {
      channels.values().forEach(Channel::clearUsers);
    }
  }

  private void onConnected() {
    sendMessageInBackground("NICKSERV", "IDENTIFY " + Hashing.md5().hashString(userService.getPassword(), UTF_8))
        .thenAccept(message -> {
          chatConnectedLatch.countDown();
          connectionState.set(ConnectionState.CONNECTED);
          joinChannel(defaultChannelName);
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(
              new PersistentNotification(i18n.get("chat.identificationFailed", throwable.getLocalizedMessage()), Severity.WARN)
          );
          return null;
        });
  }

  private <T extends Event> void addEventListener(Class<T> eventClass, ChatEventListener<T> listener) {
    if (!eventListeners.containsKey(eventClass)) {
      eventListeners.put(eventClass, new ArrayList<>());
    }
    eventListeners.get(eventClass).add(listener);
  }

  private void onChatUserList(String channelName, List<ChatUser> users) {
    getOrCreateChannel(channelName).addUsers(users);
  }

  private List<ChatUser> chatUsers(ImmutableSortedSet<User> users) {
    return users.stream().map(this::createOrGetChatUser).collect(Collectors.toList());
  }

  private void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    getOrCreateChannel(channelName).addUser(chatUser);
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
  }

  private void onModeratorSet(String channelName, String username) {
    getOrCreateChannel(channelName).setModerator(username);
  }

  @SuppressWarnings("unchecked")
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
        .setAutoReconnect(true)
        .buildConfiguration();

    pircBotX = pircBotXFactory.createPircBotX(configuration);
  }

  private void onSocialMessage(SocialMessage socialMessage) {
    socialMessage.getChannels().forEach(this::joinChannel);
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
          listener.accept(
              new ChatMessage(user.getNick(), Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage())
          );
        }
    );
  }

  public void setOnOpenPrivateChatListener(Consumer<String> onOpenPrivateChatListener) {
    this.onOpenPrivateChatListener = onOpenPrivateChatListener;
  }

  @Override
  public void connect() {
    init();

    chatConnectedLatch = new CountDownLatch(1);
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
      connectionTask.cancel();
    }
    if (pircBotX.isConnected()) {
      pircBotX.sendIRC().quitServer();
    }
  }

  @Override
  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    return taskService.submitTask(new AbstractPrioritizedTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
        updateTitle(i18n.get("chat.sendMessageTask.title"));
        pircBotX.sendIRC().message(target, message);
        return message;
      }
    });
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

        PlayerInfoBean player = playerService.getPlayerForUsername(username);
        String identiconSource = player != null ? String.valueOf(player.getId()) : username;
        if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
          notificationService.addNotification(
              new TransientNotification(
                  i18n.get("friend.nowOnlineNotification.title", username),
                  i18n.get("friend.nowOnlineNotification.action"),
                  IdenticonUtil.createIdenticon(identiconSource),
                  new Action(
                      event -> onOpenPrivateChatListener.accept(username)
                  )
              ));
        }
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
  public CompletableFuture<String> sendActionInBackground(String target, String action) {
    return taskService.submitTask(new AbstractPrioritizedTask<String>(HIGH) {
      @Override
      protected String call() throws Exception {
        updateTitle(i18n.get("chat.sendActionTask.title"));

        pircBotX.sendIRC().action(target, action);
        return action;
      }
    });
  }

  @Override
  public void joinChannel(String channelName) {
    noCatch(() -> chatConnectedLatch.await());
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
  public ChatUser createOrGetChatUser(User user) {
    synchronized (chatUsersByName) {
      String lowerUsername = user.getNick().toLowerCase(US);
      if (!chatUsersByName.containsKey(lowerUsername)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;

        if (chatPrefs.getChatColorMode() == CUSTOM && chatPrefs.getUserToColor().containsKey(lowerUsername)) {
          color = chatPrefs.getUserToColor().get(lowerUsername);
        } else if (chatPrefs.getChatColorMode() == RANDOM) {
          color = ColorGeneratorUtil.generateRandomColor(lowerUsername.hashCode());
        }

        chatUsersByName.put(lowerUsername, ChatUser.fromIrcUser(user, color));

        PlayerInfoBean player = playerService.getPlayerForUsername(user.getNick());
        String identiconSource = player != null ? String.valueOf(player.getId()) : user.getNick();
        if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
          notificationService.addNotification(
              new TransientNotification(
                  i18n.get("friend.nowOnlineNotification.title", user.getNick()),
                  i18n.get("friend.nowOnlineNotification.action"),
                  IdenticonUtil.createIdenticon(identiconSource),
                  new Action(
                      event -> onOpenPrivateChatListener.accept(user.getNick())
                  )
              ));
        }
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
