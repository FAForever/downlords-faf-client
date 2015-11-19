package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnJoinChannelsRequestListener;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Service;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.synchronizedObservableMap;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class PircBotXChatService implements ChatService, Listener, OnChatConnectedListener,
    OnChatUserListListener, OnChatUserJoinedChannelListener, OnChatUserQuitListener, OnChatDisconnectedListener,
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

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  Environment environment;

  @Autowired
  UserService userService;

  @Autowired
  TaskService taskService;

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  I18n i18n;

  @Autowired
  PircBotXFactory pircBotXFactory;

  @Autowired
  NotificationService notificationService;

  private Configuration configuration;
  private PircBotX pircBotX;
  private boolean initialized;
  private String defaultChannelName;
  private Service<Void> connectionService;
  private CountDownLatch ircConnectedLatch;
  private Map<String, ChatUser> chatUsersByName;


  public PircBotXChatService() {
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
    addOnUserListListener(this);
    addOnChatUserJoinedChannelListener(this);
    addOnChatUserQuitListener(this);
    addOnChatDisconnectedListener(this);
    addOnModeratorSetListener(this);
    addUserToColorListener();

    defaultChannelName = environment.getProperty("irc.defaultChannel");

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    chatPrefs.chatColorModeProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.equals(CUSTOM)) {
        for (ChatUser chatUser : chatUsersByName.values()) {
          if (chatPrefs.getUserToColor().containsKey(chatUser.getUsername())) {
            chatUser.setColor(chatPrefs.getUserToColor().get(chatUser.getUsername()));
          }
        }
      } else if (newValue.equals(RANDOM)) {
        for (ChatUser chatUser : chatUsersByName.values()) {
          chatUser.setColor(ColorGeneratorUtil.generateRandomHexColor());
        }
      } else {
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

  private Map<String, ChatUser> chatUsers(ImmutableSortedSet<User> users) {
    Map<String, ChatUser> chatUsers = new HashMap<>();
    for (User user : users) {
      ChatUser chatUser = createOrGetChatUser(user);
      chatUsers.put(chatUser.getUsername(), chatUser);
    }
    return chatUsers;
  }

  private Exception extractDisconnectException(DisconnectEvent event) {
    Field disconnectExceptionField = ReflectionUtils.findField(DisconnectEvent.class, "disconnectException");
    ReflectionUtils.makeAccessible(disconnectExceptionField);
    try {
      return (Exception) disconnectExceptionField.get(event);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
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
  public void onModeratorSet(String channelName, String username) {
    ChatUser chatUser = getChatUsersForChannel(channelName).get(username);
    if (chatUser == null) {
      return;
    }
    chatUser.getModeratorInChannels().add(channelName);
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
  public void addOnChatConnectedListener(final OnChatConnectedListener listener) {
    addEventListener(ConnectEvent.class, event -> listener.onConnected());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addOnUserListListener(final OnChatUserListListener listener) {
    addEventListener(UserListEvent.class,
        event -> listener.onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers())));
  }

  @Override
  public void addOnChatDisconnectedListener(final OnChatDisconnectedListener listener) {
    addEventListener(DisconnectEvent.class,
        event -> listener.onDisconnected(extractDisconnectException(event)));
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
    if (!initialized) {
      init();
    }

    connectionService = executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          try {
            ircConnectedLatch = new CountDownLatch(1);
            logger.info("Connecting to IRC at {}:{}", configuration.getServerHostname(), configuration.getServerPort());
            pircBotX.startBot();
          } catch (IOException | IrcException e) {
            int reconnectDelay = environment.getProperty("irc.reconnectDelay", int.class);
            logger.warn("Lost connection to IRC server, trying to reconnect in " + reconnectDelay / 1000 + "s");
            Thread.sleep(reconnectDelay);
          }
        }
        return null;
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void init() {
    String username = userService.getUsername();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(username)
        .setRealName(username)
        .setServer(environment.getProperty("irc.host"), environment.getProperty("irc.port", int.class))
        .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
        .setAutoSplitMessage(true)
        .setEncoding(StandardCharsets.UTF_8)
        .setAutoReconnect(false)
        .addListener(this)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .buildConfiguration();

    addOnChatConnectedListener(this);

    pircBotX = pircBotXFactory.createPircBotX(configuration);
    initialized = true;
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
          color = ColorGeneratorUtil.generateRandomHexColor();
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
      ircConnectedLatch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    pircBotX.sendIRC().joinChannel(channelName);
  }

  @Override
  public void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener) {
    lobbyServerAccessor.addOnJoinChannelsRequestListener(listener);
  }

  @Override
  public boolean isDefaultChannel(String channelName) {
    return defaultChannelName.equals(channelName);
  }

  @Override
  public void close() {
    if (connectionService != null) {
      Platform.runLater(connectionService::cancel);
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
          color = ColorGeneratorUtil.generateRandomHexColor();
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
  public void onConnected() {
    sendMessageInBackground("NICKSERV", "IDENTIFY " + md5Hex(userService.getPassword()))
        .thenAccept(s1 -> pircBotX.sendIRC().joinChannel(defaultChannelName))
        .exceptionally(throwable -> {
          notificationService.addNotification(
              new PersistentNotification(i18n.get("irc.identificationFailed", throwable.getLocalizedMessage()), Severity.WARN)
          );
          return null;
        });
  }

  @Override
  public void onDisconnected(Exception e) {
    synchronized (chatUserLists) {
      chatUserLists.values().forEach(ObservableMap::clear);
      chatUserLists.clear();
    }
  }

}
