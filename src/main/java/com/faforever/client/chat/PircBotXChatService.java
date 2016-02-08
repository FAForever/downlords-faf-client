package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.SocialMessage;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.hash.Hashing;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
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
import org.pircbotx.hooks.Listener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.synchronizedObservableMap;

public class PircBotXChatService implements ChatService, Listener,
    OnChatUserListListener, OnChatUserJoinedChannelListener, OnChatUserQuitListener,
    OnChatUserLeftChannelListener, OnModeratorSetListener {

  interface ChatEventListener<T> {

    void onEvent(T event);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SOCKET_TIMEOUT = 10000;
  private final Map<Class<? extends Event>, ArrayList<ChatEventListener>> eventListeners;

  /**
   * Maps channel names to a map containing chat users, indexed by their login name.
   */
  private final ObservableMap<String, ObservableMap<String, ChatUser>> chatUserLists;

  @Resource
  PreferencesService preferencesService;
  @Resource
  UserService userService;
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
  ExecutorService executorService;

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
  private CountDownLatch chatConnectedLatch;
  private Map<String, ChatUser> chatUsersByName;
  private Task<Void> connectionTask;
  private ObjectProperty<ConnectionState> connectionState;


  public PircBotXChatService() {
    connectionState = new SimpleObjectProperty<>();
    eventListeners = new ConcurrentHashMap<>();
    chatUserLists = observableHashMap();
    chatUsersByName = new HashMap<>();
  }

  @Override
  public void onChatUserList(String channelName, Map<String, ChatUser> users) {
    ObservableMap<String, ChatUser> chatUsersForChannel = getChatUsersForChannel(channelName);
    synchronized (chatUsersForChannel) {
      chatUsersForChannel.putAll(users);
    }
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    ObservableMap<String, ChatUser> chatUsers = getChatUsersForChannel(channelName);
    synchronized (chatUsers) {
      chatUsers.put(chatUser.getUsername(), chatUser);
    }
  }

  @Override
  public void onChatUserLeftChannel(String username, String channelName) {
    ObservableMap<String, ChatUser> chatUsersForChannel = getChatUsersForChannel(channelName);
    synchronized (chatUsersForChannel) {
      chatUsersForChannel.remove(username);
    }
  }

  @Override
  public void onChatUserQuit(String username) {
    synchronized (chatUserLists) {
      for (ObservableMap<String, ChatUser> chatUsers : chatUserLists.values()) {
        chatUsers.remove(username);
      }
    }
  }

  @PostConstruct
  void postConstruct() {
    addEventListener(ConnectEvent.class, event -> connectionState.set(ConnectionState.CONNECTED));
    addEventListener(DisconnectEvent.class, event -> connectionState.set(ConnectionState.DISCONNECTED));

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

    addOnUserListListener(this);
    addOnChatUserJoinedChannelListener(this);
    addOnChatUserQuitListener(this);
    addOnModeratorSetListener(this);
    addUserToColorListener();

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        connect();
      } else {
        disconnect();
      }
    });

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    chatPrefs.chatColorModeProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case CUSTOM:
          chatUsersByName.values().stream()
              .filter(chatUser -> chatPrefs.getUserToColor().containsKey(chatUser.getUsername()))
              .forEach(chatUser -> chatUser.setColor(chatPrefs.getUserToColor().get(chatUser.getUsername())));
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
    });
  }

  private <T extends Event> void addEventListener(Class<T> eventClass, ChatEventListener<T> listener) {
    if (!eventListeners.containsKey(eventClass)) {
      eventListeners.put(eventClass, new ArrayList<>());
    }
    eventListeners.get(eventClass).add(listener);
  }

  private void onDisconnected() {
    synchronized (chatUserLists) {
      chatUserLists.values().forEach(ObservableMap::clear);
    }
  }

  private void onConnected() {
    sendMessageInBackground("NICKSERV", "IDENTIFY " + Hashing.md5().hashString(userService.getPassword(), UTF_8))
        .thenAccept(s1 -> {
          chatConnectedLatch.countDown();
          joinChannel(defaultChannelName);
        })
        .exceptionally(throwable -> {
          notificationService.addNotification(
              new PersistentNotification(i18n.get("chat.identificationFailed", throwable.getLocalizedMessage()), Severity.WARN)
          );
          return null;
        });
  }

  private Map<String, ChatUser> chatUsers(ImmutableSortedSet<User> users) {
    Map<String, ChatUser> chatUsers = new HashMap<>();
    for (User user : users) {
      ChatUser chatUser = createOrGetChatUser(user);
      chatUsers.put(chatUser.getUsername(), chatUser);
    }
    return chatUsers;
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
        .setAutoReconnect(false)
        .addListener(this)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .buildConfiguration();

    pircBotX = pircBotXFactory.createPircBotX(configuration);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onEvent(Event event) throws Exception {
    if (!eventListeners.containsKey(event.getClass())) {
      return;
    }

    for (ChatEventListener listener : eventListeners.get(event.getClass())) {
      listener.onEvent(event);
    }
  }

  @Override
  public void addOnMessageListener(final OnChatMessageListener listener) {
    addEventListener(MessageEvent.class, event -> listener.onMessage(event.getChannel().getName(),
        new ChatMessage(
            Instant.ofEpochMilli(event.getTimestamp()),
            event.getUser().getNick(),
            event.getMessage()
        )
    ));
    addEventListener(ActionEvent.class, event -> {
      listener.onMessage(event.getChannel().getName(),
          new ChatMessage(
              Instant.ofEpochMilli(event.getTimestamp()),
              event.getUser().getNick(),
              event.getMessage(),
              true
          )
      );
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addOnUserListListener(final OnChatUserListListener listener) {
    addEventListener(UserListEvent.class,
        event -> {
          try {
            listener.onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers()));
          } catch (Exception e) {
            logger.warn("Error while handling chat user list", e);
          }
        });
  }

  @Override
  public void addOnPrivateChatMessageListener(final OnPrivateChatMessageListener listener) {
    addEventListener(PrivateMessageEvent.class,
        event -> listener.onPrivateMessage(
            event.getUser().getNick(),
            new ChatMessage(
                Instant.ofEpochMilli(event.getTimestamp()),
                event.getUser().getNick(),
                event.getMessage()
            )
        )
    );
  }

  @Override
  public void addOnChatUserJoinedChannelListener(final OnChatUserJoinedChannelListener listener) {
    addEventListener(JoinEvent.class, event -> {
      User user = event.getUser();
      listener.onUserJoinedChannel(
          event.getChannel().getName(),
          createOrGetChatUser(user)
      );
    });
  }

  @Override
  public void addOnChatUserLeftChannelListener(OnChatUserLeftChannelListener listener) {
    addEventListener(PartEvent.class, event -> listener.onChatUserLeftChannel(event.getUser().getNick(), event.getChannel().getName()));
  }

  @Override
  public void addOnModeratorSetListener(OnModeratorSetListener listener) {
    addEventListener(OpEvent.class, event -> listener.onModeratorSet(event.getChannel().getName(), event.getRecipient().getNick()));
  }

  @Override
  public void addOnChatUserQuitListener(final OnChatUserQuitListener listener) {
    addEventListener(QuitEvent.class,
        event -> listener.onChatUserQuit(
            event.getUser().getNick()
        ));
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
            chatConnectedLatch = new CountDownLatch(1);
            Configuration.ServerEntry server = configuration.getServers().get(0);
            logger.info("Connecting to IRC at {}:{}", server.getHostname(), server.getPort());
            pircBotX.startBot();
          } catch (IOException | IrcException | RuntimeException e) {
            // TODO PircBotX 2.1 supports reconnect delay
            logger.warn("Lost connection to IRC server, trying to reconnect in " + reconnectDelay / 1000 + "s");
            connectionState.set(ConnectionState.DISCONNECTED);
            Thread.sleep(reconnectDelay);
          }
        }
        return null;
      }
    };
    executorService.submit(connectionTask);
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
  public ObservableMap<String, ChatUser> getChatUsersForChannel(String channelName) {
    synchronized (chatUserLists) {
      if (!chatUserLists.containsKey(channelName)) {
        chatUserLists.put(channelName, synchronizedObservableMap(observableHashMap()));
      }
      return chatUserLists.get(channelName);
    }
  }

  @Override
  public ChatUser createOrGetChatUser(String username) {
    synchronized (chatUsersByName) {
      if (!chatUsersByName.containsKey(username)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;

        if (chatPrefs.getChatColorMode().equals(CUSTOM) && chatPrefs.getUserToColor().containsKey(username)) {
          color = chatPrefs.getUserToColor().get(username);
        } else if (chatPrefs.getChatColorMode().equals(RANDOM)) {
          color = ColorGeneratorUtil.generateRandomColor(username.hashCode());
        }

        chatUsersByName.put(username, new ChatUser(username, color));
      }
      return chatUsersByName.get(username);
    }
  }

  @Override
  public void addChannelUserListListener(String channelName, MapChangeListener<String, ChatUser> listener) {
    ObservableMap<String, ChatUser> chatUsersForChannel = getChatUsersForChannel(channelName);
    synchronized (chatUsersForChannel) {
      chatUsersForChannel.addListener(listener);
    }
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
    try {
      chatConnectedLatch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    pircBotX.sendIRC().joinChannel(channelName);
  }

  @Override
  public void addOnJoinChannelsRequestListener(Consumer<List<String>> listener) {
    fafService.addOnMessageListener(SocialMessage.class, socialMessage -> listener.accept(socialMessage.getAutoJoin()));
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
      String username = user.getNick();
      if (!chatUsersByName.containsKey(username)) {
        ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
        Color color = null;

        if (chatPrefs.getChatColorMode().equals(CUSTOM) && chatPrefs.getUserToColor().containsKey(username)) {
          color = chatPrefs.getUserToColor().get(username);
        } else if (chatPrefs.getChatColorMode().equals(RANDOM)) {
          color = ColorGeneratorUtil.generateRandomColor(username.hashCode());
        }

        chatUsersByName.put(username, ChatUser.fromIrcUser(user, color));
      }
      return chatUsersByName.get(username);
    }
  }

  @Override
  public void addUserToColorListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatPrefs.userToColorProperty().addListener((MapChangeListener<? super String, ? super Color>) change -> {
      preferencesService.store();
    });
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
  public void onModeratorSet(String channelName, String username) {
    ChatUser chatUser = getChatUsersForChannel(channelName).get(username);
    if (chatUser == null) {
      return;
    }
    chatUser.getModeratorInChannels().add(channelName);
  }
}
