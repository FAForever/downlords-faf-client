package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.google.common.collect.ImmutableSortedSet;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
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
import java.util.concurrent.ConcurrentHashMap;

import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class PircBotXChatService implements ChatService, Listener, OnChatConnectedListener,
    OnChatUserListListener, OnChatUserJoinedChannelListener, OnChatUserQuitListener, OnChatDisconnectedListener,
    OnChatUserLeftChannelListener {

  interface ChatEventListener<T> {

    void onEvent(T event);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int RECONNECT_DELAY = 3000;
  private static final int SOCKET_TIMEOUT = 10000;
  private final Map<Class<? extends Event>, ArrayList<ChatEventListener>> eventListeners;

  /**
   * Map channel names to a map containing chat users, indexed by their login name.
   */
  private final ObservableMap<String, ObservableMap<String, ChatUser>> chatUserLists;

  @Autowired
  Environment environment;

  @Autowired
  UserService userService;

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  private Configuration configuration;
  private PircBotX pircBotX;
  private boolean initialized;

  public PircBotXChatService() {
    eventListeners = new ConcurrentHashMap<>();
    chatUserLists = FXCollections.observableHashMap();
  }

  @Override
  public void onChatUserList(String channelName, Map<String, ChatUser> users) {
    synchronized (chatUserLists) {
      ObservableMap<String, ChatUser> chatUsers = getChatUsersForChannel(channelName);
      synchronized (chatUsers) {
        chatUsers.putAll(users);
      }
    }
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    synchronized (chatUserLists) {
      ObservableMap<String, ChatUser> chatUsers = getChatUsersForChannel(channelName);
      synchronized (chatUsers) {
        chatUsers.put(chatUser.getUsername(), chatUser);
      }
    }
  }

  @Override
  public void onChatUserLeftChannel(String username, String channelName) {
    getChatUsersForChannel(channelName).remove(username);
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
      ChatUser chatUser = new ChatUser(user.getLogin(), user.isIrcop());
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
  public void addOnMessageListener(final OnChatMessageListener listener) {
    addEventListener(MessageEvent.class, event -> {
      listener.onMessage(event.getChannel().getName(),
          new ChatMessage(
              Instant.ofEpochMilli(event.getTimestamp()),
              event.getUser().getLogin(),
              event.getMessage()
          )
      );
    });
    addEventListener(ActionEvent.class, event -> {
      listener.onMessage(event.getChannel().getName(),
          new ChatMessage(
              Instant.ofEpochMilli(event.getTimestamp()),
              event.getUser().getLogin(),
              event.getMessage(),
              true
          )
      );
    });
  }

  @Override
  public void addOnChatConnectedListener(final OnChatConnectedListener listener) {
    addEventListener(ConnectEvent.class,
        event -> listener.onConnected());
  }

  @Override
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
            event.getUser().getLogin(),
            new ChatMessage(
                Instant.ofEpochMilli(event.getTimestamp()),
                event.getUser().getLogin(),
                event.getMessage()
            )
        )
    );
  }

  @Override
  public void addOnChatUserJoinedChannelListener(final OnChatUserJoinedChannelListener listener) {
    addEventListener(JoinEvent.class,
        event -> listener.onUserJoinedChannel(
            event.getChannel().getName(),
            new ChatUser(event.getUser().getLogin())
        ));
  }

  @Override
  public void addOnChatUserLeftChannelListener(OnChatUserLeftChannelListener listener) {
    addEventListener(PartEvent.class, event -> {
      listener.onChatUserLeftChannel(event.getUser().getLogin(), event.getChannel().getName());
    });
  }


  @Override
  public void addOnChatUserQuitListener(final OnChatUserQuitListener listener) {
    addEventListener(QuitEvent.class,
        event -> listener.onChatUserQuit(
            event.getUser().getLogin()
        ));
  }

  @Override
  public void connect() {
    if (!initialized) {
      init();
    }

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          try {
            logger.info("Connecting to IRC at {}:{}", configuration.getServerHostname(), configuration.getServerPort());
            pircBotX.startBot();
          } catch (IOException e) {
            logger.warn("Lost connection to IRC server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
            Thread.sleep(RECONNECT_DELAY);
          }
        }
        return null;
      }
    });
  }

  private void init() {
    String username = userService.getUsername();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(username)
        .setServer(environment.getProperty("irc.host"), environment.getProperty("irc.port", Integer.class))
        .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
        .setAutoSplitMessage(true)
        .setEncoding(StandardCharsets.UTF_8)
        .setAutoReconnect(false)
        .addListener(this)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .buildConfiguration();

    addOnChatConnectedListener(this);

    pircBotX = new PircBotX(configuration);
    initialized = true;
  }

  @Override
  public void sendMessageInBackground(String target, String message, Callback<String> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<String>(i18n.get("chat.sendMessageTask.title"), HIGH) {
      @Override
      protected String call() throws Exception {
        pircBotX.sendIRC().message(target, message);
        return message;
      }
    }, callback);
  }

  @Override
  public ObservableMap<String, ChatUser> getChatUsersForChannel(String channelName) {
    synchronized (chatUserLists) {
      if (!chatUserLists.containsKey(channelName)) {
        chatUserLists.put(channelName, FXCollections.observableHashMap());
      }
      return chatUserLists.get(channelName);
    }
  }

  @Override
  public void addChannelUserListListener(String channelName, MapChangeListener<String, ChatUser> listener) {
    getChatUsersForChannel(channelName).addListener(listener);
  }

  @Override
  public void leaveChannel(String channelName) {
    pircBotX.getUserChannelDao().getChannel(channelName).send().part();
  }

  @Override
  public void sendActionInBackground(String target, String action, Callback<String> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<String>(i18n.get("chat.sendActionTask.title")) {
      @Override
      protected String call() throws Exception {
        pircBotX.sendIRC().action(target, action);
        return action;
      }
    }, callback);
  }

  @Override
  public void joinChannel(String channelName) {
    pircBotX.sendIRC().joinChannel(channelName);
  }

  @Override
  public void onEvent(Event event) throws Exception {
    if (!eventListeners.containsKey(event.getClass())) {
      return;
    }

    for (ChatEventListener listener : eventListeners.get(event.getClass())) {
      listener.onEvent(event);
    }
  }

  @Override
  public void onConnected() {
    sendMessageInBackground("NICKSERV", "IDENTIFY " + DigestUtils.md5Hex(userService.getPassword()), new Callback<String>() {
      @Override
      public void success(String message) {
        pircBotX.sendIRC().joinChannel(environment.getProperty("irc.defaultChannel"));
      }

      @Override
      public void error(Throwable e) {
        throw new RuntimeException(e);
      }
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
