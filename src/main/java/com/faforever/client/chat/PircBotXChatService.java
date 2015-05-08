package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import javafx.application.Platform;
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
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class PircBotXChatService implements ChatService, Listener, OnConnectedListener {

  interface ChatEventListener<T> {

    void onEvent(T event);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int RECONNECT_DELAY = 3000;

  @Autowired
  private Environment environment;

  @Autowired
  private UserService userService;

  private Map<Class<? extends Event>, ArrayList<ChatEventListener>> eventListeners;
  private Configuration configuration;
  private PircBotX pircBotX;
  private boolean initialized;

  public PircBotXChatService() {
    eventListeners = new HashMap<>();
  }

  void init() {
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
  public void addOnServerResponseListener(final OnServerResponseListener listener) {
    addEventListener(ServerResponseEvent.class,
        event -> listener.onServerResponse());
  }

  @Override
  public void addOnUserListListener(final OnUserListListener listener) {
    addEventListener(UserListEvent.class,
        event -> listener.onChatUserList(event.getChannel().getName(), playerInfoBeans(event.getUsers())));
  }

  @Override
  public void addOnDisconnectedListener(final OnDisconnectedListener listener) {
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
  public void addOnUserJoinedListener(final OnUserJoinedListener listener) {
    addEventListener(JoinEvent.class,
        event -> listener.onChannelJoined(
            event.getChannel().getName(),
            new PlayerInfoBean(event.getUser())
        ));
  }

  @Override
  public void addOnUserLeftListener(final OnUserLeftListener listener) {
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
      Platform.runLater(() -> listener.onEvent(event));
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

  private Map<String, PlayerInfoBean> playerInfoBeans(ImmutableSortedSet<User> users) {
    Map<String, PlayerInfoBean> playerInfoBeans = new HashMap<>(users.size(), 1);
    for (User user : users) {
      PlayerInfoBean playerInfoBean = new PlayerInfoBean(user);
      playerInfoBeans.put(playerInfoBean.getUsername(), playerInfoBean);
    }
    return playerInfoBeans;
  }
}
