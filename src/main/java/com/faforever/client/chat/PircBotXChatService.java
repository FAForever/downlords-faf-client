package com.faforever.client.chat;

import com.faforever.client.chat.jan.event.ChatEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Irc;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
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
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

@Service
@Slf4j
public class PircBotXChatService implements DisposableBean, Cloneable {
  
  private static final List<UserLevel> MODERATOR_USER_LEVELS = Arrays.asList(UserLevel.OP, UserLevel.HALFOP, UserLevel.SUPEROP, UserLevel.OWNER);
  private static final int SOCKET_TIMEOUT = 10000;
  
  @VisibleForTesting
  final ObjectProperty<ConnectionState> connectionState;
  private final Map<Class<GenericEvent>, ArrayList<Consumer<GenericEvent>>> eventListeners;

  private final UserService userService;
  private final TaskService taskService;
  private final I18n i18n;
  private final PircBotXFactory pircBotXFactory;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final ClientProperties clientProperties;
  private String defaultChannelName;

  private Configuration configuration;
  private PircBotX pircBotX;
  /** Called when the IRC server has confirmed our identity. */
  private CompletableFuture<Void> identifiedFuture;
  private Task<Void> connectionTask;
  
  Runnable onChatLoginSuccess;

  @Inject
  public PircBotXChatService(UserService userService, TaskService taskService,
                             FafService fafService, I18n i18n, PircBotXFactory pircBotXFactory,
                             @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ThreadPoolExecutor threadPoolExecutor,
                             ClientProperties clientProperties) {
    this.userService = userService;
    this.taskService = taskService;
    this.i18n = i18n;
    this.pircBotXFactory = pircBotXFactory;
    this.threadPoolExecutor = threadPoolExecutor;
    this.clientProperties = clientProperties;

    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    eventListeners = new ConcurrentHashMap<>();
    identifiedFuture = new CompletableFuture<>();
    
    init();
  }

  // Alternatively, instead of directly creating Consumer objects, one simple data wrapper classes
  // could be created per Event, which then gets passed to a Consumer<SimpleWrapperClassForEventFoo>.
  
  // TODO check if ChatUser should know its own channel name or not -> what does getOrCreateChatUser do exactly?
  
  @FunctionalInterface public interface ConsumerUsername extends ChatEvent, Consumer<String> {}
  @FunctionalInterface public interface ConsumerChannelNameAndUsername extends ChatEvent, BiConsumer<String, String> {}
  @FunctionalInterface public interface ConsumerChannelNameAndChatUserList extends ChatEvent, BiConsumer<String, Set<ChatUser>> {}
  @FunctionalInterface public interface ConsumerChannelNameAndChannelTopic extends ChatEvent, BiConsumer<String, String> {}
  @FunctionalInterface public interface ConsumerChannelNameAndChatUser extends ChatEvent, BiConsumer<String, ChatUser> {}
  
  private void onEvent(Event event) {
    if (!eventListeners.containsKey(event.getClass())) {
      log.trace("No event listener for: {}", event);
      return;
    }
    eventListeners.get(event.getClass()).forEach(listener -> listener.accept(event));
  }
  
  private <T extends GenericEvent> void addEventListener(Class<T> eventClass, Consumer<T> listener) {
    @SuppressWarnings("unchecked")
    Class<GenericEvent> clazz = (Class<GenericEvent>) eventClass;
    @SuppressWarnings("unchecked")
    Consumer<GenericEvent> lizzener = (Consumer<GenericEvent>) listener;
    eventListeners.computeIfAbsent(clazz, aClass -> new ArrayList<>()).add(lizzener);
  }
  
  private void init() {
    addEventListener(NoticeEvent.class, this::onNotice);
    addEventListener(MotdEvent.class, this::onMotd);
    addEventListener(ConnectEvent.class, event -> connectionState.set(ConnectionState.CONNECTED));
    addEventListener(DisconnectEvent.class, event -> connectionState.set(ConnectionState.DISCONNECTED));
  }
  

  public void registerEventHandlers(
      /** Called when we are logged in => allowed to join rooms */
      Runnable onChatLoginSuccess
      ) {
    this.onChatLoginSuccess = onChatLoginSuccess;
  }
  
  public void registerChatRoomEventHandlers(
      /** Called when we receive the list of current users of a joined room (usually after joining a new one) */
      ConsumerChannelNameAndChatUserList onChannelUsersReceived,
      /** Called when we receive the topic of a joined room (after joining and when it changes) */
      ConsumerChannelNameAndChannelTopic onChannelTopicChanged,
      /** Called when a user joins a room joined by us*/
      ConsumerChannelNameAndChatUser onUserJoinedChannel,
      /** Called when the channel role of a user inside one of our joined rooms changes*/
      ConsumerChannelNameAndChatUser onUserRoleChanged,
      /** Called when a user leaves a room joined by us */
      ConsumerChannelNameAndUsername onUserLeftChannel,
      /** Called when a user quits (leaves all rooms) (the user must have been in at least one room joined by us) */
      ConsumerUsername onUserQuitChat,
      /** Called when a user from a room we joined writes something in that room */
      Consumer<ChatMessage> onChatMessageReceived
      ) {
    addEventListener(UserListEvent.class, event -> convertUserListEvent(event, onChannelUsersReceived));
    addEventListener(TopicEvent.class, event -> onChannelTopicChanged.accept(event.getChannel().getName(), event.getTopic()));
    addEventListener(JoinEvent.class, event -> convertJoinEvent(event, onUserJoinedChannel));
    addEventListener(OpEvent.class, event -> convertOpEvent(event, onUserRoleChanged));
    addEventListener(PartEvent.class, event -> onUserLeftChannel.accept(event.getChannel().getName(), event.getUser().getNick()));
    addEventListener(QuitEvent.class, event -> onUserQuitChat.accept(event.getUser().getNick()));
    addEventListener(ActionEvent.class, event -> convertActionEvent(event, onChatMessageReceived));
    addEventListener(MessageEvent.class, event -> convertMessageEvent(event, onChatMessageReceived));
  }
  
  public void registerPrivateMessageEventHandler(
      /** Called when we receive a private message that is not related to any room */
      Consumer<ChatMessage> onPrivateChatMessageReceived
      ) {
    addEventListener(PrivateMessageEvent.class, event -> convertPrivateMessageEvent(event, onPrivateChatMessageReceived));
  }
  
  private void onNotice(NoticeEvent event) {
    Configuration config = event.getBot().getConfiguration();
    UserHostmask hostmask = event.getUserHostmask();
    
    String message = event.getMessage();
    long id = event.getId();
    log.trace("NoticeEvent {}: {}", id, message);

    if (config.getNickservOnSuccess() != null && containsIgnoreCase(hostmask.getHostmask(), config.getNickservNick())) {
      if (containsIgnoreCase(message, config.getNickservOnSuccess()) || containsIgnoreCase(message, "registered under your account")) {
        // We are identified already (we probably did manual identify for a previous chat room).
        log.debug("Identification successful.", id);
        onIdentified();
      } else if (message.contains("isn't registered")) {
        // Not yet registered.
        log.debug("Identification failed. Registering user.", id);
        pircBotX.sendIRC().message(config.getNickservNick(), format("register %s %s@users.faforever.com", getPassword(), userService.getUsername()));
      } else if (message.contains(" registered")) {
        // We just registered and are now identified.
        log.debug("Identification successful.", id);
        onIdentified();
      } else if (message.contains("choose a different nick")) {
        log.debug("Not identified yet. Sending identity.", id);
        // Send a private IDENTIFY message to nickserv manually, needed when entering the first chat room after a (re)connect.
        sendIdentify(config);
      }
    }
  }
  
  private void onMotd(MotdEvent event) {
    sendIdentify(event.getBot().getConfiguration());
  }

  private void sendIdentify(Configuration config) {
    pircBotX.sendIRC().message(config.getNickservNick(), format("identify %s", getPassword()));
  }
  
  private void onIdentified() {
    identifiedFuture.thenAccept(aVoid -> {
      // TODO what was the issue with double logins
      onChatLoginSuccess.run();
    });
    identifiedFuture.complete(null);
  }
  
  private void convertUserListEvent(UserListEvent event, ConsumerChannelNameAndChatUserList handler) {
    String channelName = event.getChannel().getName();
    Set<ChatUser> users = event.getUsers().stream().map(user -> new ChatUser(user.getNick(), isModeratorFor(user, channelName))).collect(Collectors.toSet());
    handler.accept(channelName, users);
  }

  private void convertJoinEvent(JoinEvent event, ConsumerChannelNameAndChatUser handler) {
    User user = Objects.requireNonNull(event.getUser());
    log.trace("User joined channel: {}", user);
    
    String username = user.getNick() != null ? user.getNick() : user.getLogin();
    String channelName = event.getChannel().getName();
    handler.accept(channelName, new ChatUser(username, isModeratorFor(user, channelName)));
  }
  
  private boolean isModeratorFor(User ircUser, String channelName) {
    return ircUser.getChannels().stream()
        .filter(channel -> channel.getName().equals(channelName))
        .flatMap(channel -> ircUser.getUserLevels(channel).stream())
        .anyMatch(MODERATOR_USER_LEVELS::contains);
  }
  
  private void convertOpEvent(OpEvent event, ConsumerChannelNameAndChatUser handler) {
    User recipient = event.getRecipient();
    if (recipient != null) {
      handler.accept(event.getChannel().getName(), new ChatUser(recipient.getNick(), true));
    }
  }

  @NotNull
  private String getPassword() {
    return Hashing.md5().hashString(Hashing.sha256().hashString(userService.getPassword(), UTF_8).toString(), UTF_8).toString();
  }

  private void convertActionEvent(ActionEvent event, Consumer<ChatMessage> handler) {
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
    handler.accept(new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), true));
  }

  private void convertMessageEvent(MessageEvent event, Consumer<ChatMessage> handler) {
    User user = event.getUser();
    if (user == null) {
      log.warn("Action event without user: {}", event);
      return;
    }

    String source;
    org.pircbotx.Channel channel = event.getChannel();
    source = channel.getName();

    handler.accept(new ChatMessage(source, Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage(), false));
  }

  private void convertPrivateMessageEvent(PrivateMessageEvent event, Consumer<ChatMessage> handler) {
    User user = event.getUser();
    if (user == null) {
      log.warn("Private message without user: {}", event);
      return;
    }
    log.debug("Received private message: {}", event);
    handler.accept(new ChatMessage(user.getNick(), Instant.ofEpochMilli(event.getTimestamp()), user.getNick(), event.getMessage()));
  }

  public void connect() {
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
            log.error("Connecting failed!", e);
          }
        }
        return null;
      }
    };
    threadPoolExecutor.execute(connectionTask);
  }

  public void disconnect() {
    if (connectionTask != null) {
      connectionTask.cancel(false);
    }
    log.info("Disconnecting from IRC");
    pircBotX.stopBotReconnect();
    if (pircBotX.isConnected()) {
      pircBotX.sendIRC().quitServer();
    }
    identifiedFuture = new CompletableFuture<>();
  }
  
  public void reconnect() {
    connect();
    disconnect();
  }

  public CompletableFuture<String> sendMessageInBackground(String target, String message) {
    return taskService.submitTask(new CompletableTask<String>(HIGH) {
      @Override
      protected String call() {
        updateTitle(i18n.get("chat.sendMessageTask.title"));
        pircBotX.sendIRC().message(target, message);
        return message;
      }
    }).getFuture();
  }

  public void leaveChannel(String channelName) {
    pircBotX.getUserChannelDao().getChannel(channelName).send().part();
  }

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

  public void joinChannel(String channelName) {
    log.debug("Joining channel (waiting for identification): {}", channelName);
    identifiedFuture.thenAccept(aVoid -> {
      log.debug("Joining channel: {}", channelName);
      pircBotX.sendIRC().joinChannel(channelName);
    });
  }

  @Override
  public void destroy() throws Exception {
    close();
  }
  
  public void close() {
    identifiedFuture.cancel(false);
    if (connectionTask != null) {
      connectionTask.cancel();
    }
    log.info("Disconnecting from IRC");
    if (pircBotX != null) {
      pircBotX.sendIRC().quitServer();
    }
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  public void whois(String username) {
    pircBotX.sendIRC().whois(username);
  }
  
  /**
   * Can only be called after {@link #connect()} has been called.
   */
  public boolean isDefaultChannel(String channelName) {
    Objects.requireNonNull(defaultChannelName);
    return defaultChannelName.equals(channelName);
  }

  /**
   * Can only be called after {@link #connect()} has been called.
   */
  public String getDefaultChannelName() {
    Objects.requireNonNull(defaultChannelName);
    return defaultChannelName;
  }
}
