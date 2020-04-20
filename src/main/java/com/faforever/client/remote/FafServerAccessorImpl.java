package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fa.CloseGameEvent;
import com.faforever.client.fa.relay.GpgClientMessageSerializer;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.UidService;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerInfoClientMessage;
import com.faforever.client.rankedmatch.SearchLadder1v1ClientMessage;
import com.faforever.client.rankedmatch.StopSearchLadder1v1ClientMessage;
import com.faforever.client.remote.domain.AddFoeMessage;
import com.faforever.client.remote.domain.AddFriendMessage;
import com.faforever.client.remote.domain.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.AvatarMessage;
import com.faforever.client.remote.domain.BanPlayerMessage;
import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.ClosePlayersFAMessage;
import com.faforever.client.remote.domain.ClosePlayersLobbyMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.HostGameMessage;
import com.faforever.client.remote.domain.IceServersServerMessage;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.InitSessionMessage;
import com.faforever.client.remote.domain.JoinGameMessage;
import com.faforever.client.remote.domain.ListIceServersMessage;
import com.faforever.client.remote.domain.ListPersonalAvatarsMessage;
import com.faforever.client.remote.domain.LoginClientMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MakeBroadcastMessage;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.PingMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.RemoveFoeMessage;
import com.faforever.client.remote.domain.RemoveFriendMessage;
import com.faforever.client.remote.domain.RestoreGameSessionMessage;
import com.faforever.client.remote.domain.SelectAvatarMessage;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.ServerCommand;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.remote.domain.SessionMessage;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.GameAccessTypeAdapter;
import com.faforever.client.remote.gson.GameStateTypeAdapter;
import com.faforever.client.remote.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.faforever.client.remote.gson.RatingRangeTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.update.Version;
import com.github.nocatch.NoCatch;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;
import static java.nio.charset.StandardCharsets.UTF_8;

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
@Slf4j
public class FafServerAccessorImpl extends AbstractServerAccessor implements FafServerAccessor,
    InitializingBean, DisposableBean {

  private Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
      .registerTypeAdapter(GameStatus.class, GameStateTypeAdapter.INSTANCE)
      .registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
      .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
      .registerTypeAdapter(ServerMessage.class, ServerMessageTypeAdapter.INSTANCE)
      .registerTypeAdapter(RatingRange.class, RatingRangeTypeAdapter.INSTANCE)
      .create();
  private final HashMap<Class<? extends ServerMessage>, Collection<Consumer<ServerMessage>>> messageListeners = new HashMap<>();

  private final PreferencesService preferencesService;
  private final UidService uidService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final TaskScheduler taskScheduler;
  private final EventBus eventBus;
  private final ReconnectTimerService reconnectTimerService;

  @org.jetbrains.annotations.NotNull
  private final ClientProperties clientProperties;
  private Task<Void> fafConnectionTask;
  private String localIp;
  private ServerWriter serverWriter;
  private volatile CompletableFuture<LoginMessage> loginFuture;
  private CompletableFuture<SessionMessage> sessionFuture;
  private CompletableFuture<GameLaunchMessage> gameLaunchFuture;
  private ObjectProperty<Long> sessionId = new SimpleObjectProperty<>();
  private String username;
  private String password;
  private ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>();
  private Socket fafServerSocket;
  private CompletableFuture<List<Avatar>> avatarsFuture;
  private CompletableFuture<List<IceServer>> iceServersFuture;

  private void onAvatarMessage(AvatarMessage avatarMessage) {
    avatarsFuture.complete(avatarMessage.getAvatarList());
  }

  private void onIceServersMessage(IceServersServerMessage iceServersServerMessage) {
    iceServersFuture.complete(iceServersServerMessage.getIceServers());
  }

  private void onNotice(NoticeMessage noticeMessage) {
    if (Objects.equals(noticeMessage.getStyle(), "kill")) {
      log.warn("Game close requested by server...");
      notificationService.addNotification(new ImmediateNotification(i18n.get("game.kicked.title"), i18n.get("game.kicked.message", clientProperties.getLinks().get("linksRules")), Severity.ERROR, Collections.singletonList(new DismissAction(i18n))));
      eventBus.post(new CloseGameEvent());
    }

    if (Objects.equals(noticeMessage.getStyle(), "kick")) {
      log.warn("Kicked from lobby, client closing after delay...");
      notificationService.addNotification(new ImmediateNotification(i18n.get("server.kicked.title"), i18n.get("server.kicked.message", clientProperties.getLinks().get("linksRules")), Severity.ERROR, Collections.singletonList(new DismissAction(i18n))));
      taskScheduler.scheduleWithFixedDelay(Platform::exit, Duration.ofSeconds(10));
    }

    if (noticeMessage.getText() == null) {
      return;
    }
    notificationService.addNotification(new ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.getText(), noticeMessage.getSeverity(),
        Collections.singletonList(new DismissAction(i18n))));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<ServerMessage>) listener);
  }

  @Override
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    sessionFuture = new CompletableFuture<>();
    loginFuture = new CompletableFuture<>();
    this.username = username;
    this.password = password;

    // TODO extract class?
    fafConnectionTask = new Task<Void>() {

      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          Server server = clientProperties.getServer();
          String serverHost = server.getHost();
          int serverPort = server.getPort();

          log.info("Trying to connect to FAF server at {}:{}", serverHost, serverPort);
          Platform.runLater(() -> connectionState.set(ConnectionState.CONNECTING));


          try (Socket fafServerSocket = new Socket(serverHost, serverPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {
            FafServerAccessorImpl.this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            writeToServer(new InitSessionMessage(Version.getCurrentVersion()));

            log.info("FAF server connection established");
            Platform.runLater(() -> connectionState.set(ConnectionState.CONNECTED));
            reconnectTimerService.resetConnectionFailures();

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            Platform.runLater(() -> connectionState.set(ConnectionState.DISCONNECTED));
            if (isCancelled()) {
              log.debug("Connection to FAF server has been closed");
            } else {
              log.warn("Lost connection to Server", e);
              reconnectTimerService.incrementConnectionFailures();
              reconnectTimerService.waitForReconnect();
            }
          }
        }
        return null;
      }

      @Override
      protected void cancelled() {
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(fafServerSocket);
        log.debug("Closed connection to FAF lobby server");
      }
    };
    executeInBackground(fafConnectionTask);
    return loginFuture;
  }


  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    HostGameMessage hostGameMessage = new HostGameMessage(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        new boolean[0],
        newGameInfo.getFeaturedMod().getTechnicalName(),
        newGameInfo.getPassword(),
        null,
        newGameInfo.getGameVisibility()
    );

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(hostGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    JoinGameMessage joinGameMessage = new JoinGameMessage(gameId, password);

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(joinGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public void destroy() {
    disconnect();
  }

  public void disconnect() {
    if (fafConnectionTask != null) {
      fafConnectionTask.cancel(true);
    }
  }

  @Override
  public void reconnect() {
    IOUtils.closeQuietly(fafServerSocket);
    reconnectTimerService.skipWait();
  }

  @Override
  public void addFriend(int playerId) {
    writeToServer(new AddFriendMessage(playerId));
  }

  @Override
  public void addFoe(int playerId) {
    writeToServer(new AddFoeMessage(playerId));
  }

  @Override
  public void requestMatchmakerInfo() {
    writeToServer(new MatchmakerInfoClientMessage());
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction) {
    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(new SearchLadder1v1ClientMessage(faction));
    return gameLaunchFuture;
  }

  @Override
  public void stopSearchingRanked() {
    writeToServer(new StopSearchLadder1v1ClientMessage());
    gameLaunchFuture = null;
  }

  @Override
  public void sendGpgMessage(GpgGameMessage message) {
    writeToServer(message);
  }

  @Override
  public void removeFriend(int playerId) {
    writeToServer(new RemoveFriendMessage(playerId));
  }

  @Override
  public void removeFoe(int playerId) {
    writeToServer(new RemoveFoeMessage(playerId));
  }

  @Override
  public void selectAvatar(URL url) {
    writeToServer(new SelectAvatarMessage(url));
  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    writeToServer(new BanPlayerMessage(playerId, reason, duration, periodType.name()));
  }

  @Override
  public void closePlayersGame(int playerId) {
    writeToServer(new ClosePlayersFAMessage(playerId));
  }

  @Override
  public void closePlayersLobby(int playerId) {
    writeToServer(new ClosePlayersLobbyMessage(playerId));
  }

  @Override
  public void broadcastMessage(String message) {
    writeToServer(new MakeBroadcastMessage(message));
  }

  @Override
  @Cacheable(CacheNames.AVAILABLE_AVATARS)
  public List<Avatar> getAvailableAvatars() {
    avatarsFuture = new CompletableFuture<>();
    writeToServer(new ListPersonalAvatarsMessage());
    return NoCatch.noCatch(() -> avatarsFuture.get(10, TimeUnit.SECONDS));
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    iceServersFuture = new CompletableFuture<>();
    writeToServer(new ListIceServersMessage());
    return iceServersFuture;
  }

  @Override
  public void restoreGameSession(int id) {
    writeToServer(new RestoreGameSessionMessage(id));
  }

  private ServerWriter createServerWriter(OutputStream outputStream) {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(), ClientMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    serverWriter.registerMessageSerializer(new GpgClientMessageSerializer(), GpgGameMessage.class);
    return serverWriter;
  }

  private void writeToServer(SerializableMessage message) {
    final CompletableFuture loginFuture = this.loginFuture;
    if (message instanceof GpgGameMessage && loginFuture != null && !loginFuture.isDone()) {
      log.warn("GPGNetMessage discarded due to not being logged in");
      return;
    }

    serverWriter.write(message);
  }

  public void onServerMessage(String message) {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      dispatchServerMessage(serverCommand);
    } else {
      parseServerObject(message);
    }
  }

  private void dispatchServerMessage(ServerCommand serverCommand) {
    switch (serverCommand) {
      case PING:
        log.debug("Server PINGed");
        onServerPing();
        break;

      case PONG:
        log.debug("Server PONGed");
        break;

      default:
        log.warn("Unknown server response: {}", serverCommand);
    }
  }

  private void parseServerObject(String jsonString) {
    try {
      ServerMessage serverMessage = gson.fromJson(jsonString, ServerMessage.class);
      if (serverMessage == null) {
        log.debug("Discarding unimplemented server message: {}", jsonString);
        return;
      }

      Class<?> messageClass = serverMessage.getClass();
      while (messageClass != Object.class) {
        messageListeners.getOrDefault(messageClass, Collections.emptyList())
            .forEach(consumer -> consumer.accept(serverMessage));
        messageClass = messageClass.getSuperclass();
      }
      for (Class<?> type : ClassUtils.getAllInterfacesForClassAsSet(messageClass)) {
        messageListeners.getOrDefault(messageClass, Collections.emptyList())
            .forEach(consumer -> consumer.accept(serverMessage));
      }
    } catch (JsonSyntaxException e) {
      log.warn("Could not deserialize message: " + jsonString, e);
    }
  }

  private void onServerPing() {
    writeToServer(new PongMessage());
  }

  private void dispatchAuthenticationFailed(AuthenticationFailedMessage message) {
    fafConnectionTask.cancel();
    loginFuture.completeExceptionally(new LoginFailedException(message.getText()));
    loginFuture = null;
  }

  private void onFafLoginSucceeded(LoginMessage loginServerMessage) {
    log.info("FAF login succeeded");

    if (loginFuture != null) {
      loginFuture.complete(loginServerMessage);
      loginFuture = null;
    }
  }

  private void onSessionInitiated(SessionMessage sessionMessage) {
    log.info("FAF session initiated, session ID: {}", sessionMessage.getSession());
    this.sessionId.set(sessionMessage.getSession());
    sessionFuture.complete(sessionMessage);
    logIn(username, password);
  }

  private void logIn(String username, String password) {
    try {
      String uniqueId = uidService.generate(String.valueOf(sessionId.get()), preferencesService.getFafDataDirectory().resolve("uid.log"));
      writeToServer(new LoginClientMessage(username, Hashing.sha256().hashString(password, UTF_8).toString(), sessionId.get(), uniqueId, localIp));
    } catch (IOException e) {
      onUIDNotExecuted(e);
    }
  }

  @VisibleForTesting
  protected void onUIDNotExecuted(Exception e) {
    log.error("UID.exe not executed", e);
    if (e.getMessage() == null) {
      return;
    }
    notificationService.addNotification(new ImmediateNotification(i18n.get("UIDNotExecuted"), e.getMessage(), Severity.ERROR,
        Collections.singletonList(new ReportAction(i18n, reportingService, e))));
  }

  private void onGameLaunchInfo(GameLaunchMessage gameLaunchMessage) {
    gameLaunchFuture.complete(gameLaunchMessage);
    gameLaunchFuture = null;
  }

  @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
  @Override
  public void ping() {
    if (fafServerSocket == null || !fafServerSocket.isConnected() || serverWriter == null) {
      return;
    }
    writeToServer(PingMessage.INSTANCE);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    addOnMessageListener(NoticeMessage.class, this::onNotice);
    addOnMessageListener(SessionMessage.class, this::onSessionInitiated);
    addOnMessageListener(LoginMessage.class, this::onFafLoginSucceeded);
    addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchInfo);
    addOnMessageListener(AuthenticationFailedMessage.class, this::dispatchAuthenticationFailed);
    addOnMessageListener(AvatarMessage.class, this::onAvatarMessage);
    addOnMessageListener(IceServersServerMessage.class, this::onIceServersMessage);
  }
}
