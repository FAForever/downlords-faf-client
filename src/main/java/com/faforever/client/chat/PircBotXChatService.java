package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import javafx.collections.*;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class PircBotXChatService implements ChatService, Listener, OnConnectedListener, OnChatUserListListener, OnChannelJoinedListener, OnChatUserLeftListener, OnChatDisconnectedListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int RECONNECT_DELAY = 3000;
  private static final int SOCKET_TIMEOUT = 10000;

  @Autowired
  private Environment environment;
  @Autowired
  private UserService userService;

  private final Map<Class<? extends Event>, ArrayList<ChatEventListener>> eventListeners;
  private final ObservableMap<String, ObservableSet<ChatUser>> chatUserLists;

  private Configuration configuration;
  private PircBotX pircBotX;
  private boolean initialized;

  public PircBotXChatService() {
    eventListeners = new ConcurrentHashMap<>();
    chatUserLists = FXCollections.observableHashMap();
  }

  @Override
  public void onChatUserList(String channelName, Collection<ChatUser> users) {
    synchronized (chatUserLists) {
      Set<ChatUser> chatUsers = getChatUsersForChannel(channelName);
      synchronized (chatUsers) {
        chatUsers.addAll(users);
      }
    }
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    synchronized (chatUserLists) {
      Set<ChatUser> chatUsers = getChatUsersForChannel(channelName);
      synchronized (chatUsers) {
        chatUsers.add(chatUser);
      }
    }
  }

  @Override
  public void onUserLeft(String username) {
    synchronized (chatUserLists) {
      for (ObservableSet<ChatUser> chatUsers : chatUserLists.values()) {
        chatUsers.remove(username);
      }
    }
  }

  @Override
  public ObservableSet<ChatUser> getChatUsersForChannel(String channelName) {
    if (!chatUserLists.containsKey(channelName)) {
      chatUserLists.put(channelName, FXCollections.observableSet(new HashSet<>()));
    }
    return chatUserLists.get(channelName);
  }

  @PostConstruct
  void postConstruct() {
    addOnUserListListener(this);
    addOnChannelJoinedListener(this);
    addOnChatUserLeftListener(this);
    addOnDisconnectedListener(this);
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

    addOnConnectedListener(this);

    pircBotX = new PircBotX(configuration);
    initialized = true;
  }

  @Override
  public void addOnConnectedListener(final OnConnectedListener listener) {
    addEventListener(ConnectEvent.class,
        event -> listener.onConnected());
  }

  @Override
  public void addOnUserListListener(final OnChatUserListListener listener) {
    addEventListener(UserListEvent.class,
        event -> listener.onChatUserList(event.getChannel().getName(), chatUsers(event.getUsers())));
  }

  @Override
  public void addOnDisconnectedListener(final OnChatDisconnectedListener listener) {
    addEventListener(DisconnectEvent.class,
        event -> listener.onDisconnected(extractDisconnectException(event)));
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
  public void addOnMessageListener(final OnMessageListener listener) {
    addEventListener(MessageEvent.class, event -> {
      listener.onMessage(event.getChannel().getName(),
          new ChatMessage(
              Instant.ofEpochMilli(event.getTimestamp()),
              event.getUser().getNick(),
              event.getMessage()
          )
      );
    });
  }

  @Override
  public void addOnPrivateMessageListener(final OnPrivateMessageListener listener) {
    addEventListener(PrivateMessageEvent.class,
        event -> listener.onPrivateMessage(
            event.getUser().getNick(),
            new ChatMessage(
                Instant.ofEpochMilli(event.getTimestamp()),
                event.getUser().getLogin(),
                event.getMessage()
            )
        )
    );
  }

  @Override
  public void addOnChannelJoinedListener(final OnChannelJoinedListener listener) {
    addEventListener(JoinEvent.class,
        event -> listener.onUserJoinedChannel(
            event.getChannel().getName(),
            new ChatUser(event.getUser().getNick())
        ));
  }

  @Override
  public void addOnChatUserLeftListener(final OnChatUserLeftListener listener) {
    addEventListener(QuitEvent.class,
        event -> listener.onUserLeft(
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

  @Override
  public void sendMessage(String target, String message) {
    pircBotX.sendIRC().message(target, message);
  }

  @Override
  public void addChannelUserListListener(String channelName, SetChangeListener<ChatUser> listener) {
    getChatUsersForChannel(channelName).addListener(listener);
  }

  private <T extends Event> void addEventListener(Class<T> eventClass, ChatEventListener<T> listener) {
    if (!eventListeners.containsKey(eventClass)) {
      eventListeners.put(eventClass, new ArrayList<>());
    }
    eventListeners.get(eventClass).add(listener);
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
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        sendMessage("NICKSERV", "IDENTIFY " + DigestUtils.md5Hex(userService.getPassword()));
        pircBotX.sendIRC().joinChannel(environment.getProperty("irc.defaultChannel"));
        return null;
      }
    });
  }

  private Collection<ChatUser> chatUsers(ImmutableSortedSet<User> users) {
    Collection<ChatUser> chatUsers = new ArrayList<>();
    for (User user : users) {
      ChatUser chatUser = new ChatUser(user.getNick());
      chatUsers.add(chatUser);
    }
    return chatUsers;
  }

  @Override
  public void onDisconnected(Exception e) {
    synchronized (chatUserLists) {
      chatUserLists.values().forEach(ObservableSet<ChatUser>::clear);
      chatUserLists.clear();
    }
  }

  interface ChatEventListener<T> {

    void onEvent(T event);
  }
}
