package com.faforever.client.irc;

import com.faforever.client.user.UserService;
import com.google.common.collect.ImmutableSortedSet;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class PircBotXIrcService implements IrcService, Listener, OnConnectedListener {

  interface IrcEventListener<T> {

    void onEvent(T event);
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int RECONNECT_DELAY = 6000;

  @Autowired
  private Environment environment;

  @Autowired
  private UserService userService;

  private Map<Class<? extends Event>, ArrayList<IrcEventListener>> eventListeners;
  private Configuration configuration;
  private PircBotX pircBotX;

  public PircBotXIrcService() {
    eventListeners = new HashMap<>();
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
  public void addOnDisconnectedListener(final OnDisconnectedListener listener) {
    addEventListener(DisconnectEvent.class,
        event -> listener.onDisconnected());
  }

  @Override
  public void addOnMessageListener(final OnMessageListener listener) {
    addEventListener(MessageEvent.class, event -> {
      listener.onMessage(
          event.getChannel().getName(),
          Instant.ofEpochMilli(event.getTimestamp()),
          event.getUser().getNick(),
          event.getMessage()
      );
    });
  }

  @Override
  public void addOnPrivateMessageListener(final OnPrivateMessageListener listener) {
    addEventListener(PrivateMessageEvent.class,
        event -> listener.onPrivateMessage(
            event.getUser().getNick(),
            Instant.ofEpochMilli(event.getTimestamp()),
            event.getMessage()
        ));
  }

  @Override
  public void addOnChannelJoinedListener(final OnChannelJoinedListener listener) {
    addEventListener(JoinEvent.class,
        event -> listener.onChannelJoined(
            event.getChannel().getName(),
            users(event.getChannel())
        ));
  }

  private List<IrcUser> users(Channel channel) {
    ImmutableSortedSet<User> users = channel.getUsers();
    List<IrcUser> ircUsers = new ArrayList<>(users.size());
    for (User user : users) {
      IrcUser ircUser = new IrcUser(user);
      ircUsers.add(ircUser);
    }
    return ircUsers;
  }

  @Override
  public void connect() {
    String username = userService.getUsername();

    configuration = new Configuration.Builder()
        .setName(username)
        .setLogin(username)
        .setServer(environment.getProperty("irc.host"), environment.getProperty("irc.port", Integer.class))
        .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
        .setAutoReconnect(true)
        .setAutoSplitMessage(true)
        .setEncoding(StandardCharsets.UTF_8)
        .addListener(this)
        .buildConfiguration();

    addOnConnectedListener(this);

    pircBotX = new PircBotX(configuration);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        try {
          pircBotX.startBot();
        } catch (IOException | IrcException e) {
          throw new RuntimeException(e);
        }
        return null;
      }
    });
  }

  @Override
  public void sendMessage(String target, String message) {
    pircBotX.sendIRC().message(target, message);
  }

  private <T extends Event> void addEventListener(Class<T> eventClass, IrcEventListener<T> listener) {
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

    for (IrcEventListener listener : eventListeners.get(event.getClass())) {
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
}
