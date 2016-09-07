package com.faforever.client.remote;

import com.faforever.client.config.CacheNames;
import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.UidService;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.SearchRanked1V1ClientMessage;
import com.faforever.client.rankedmatch.StopSearchRanked1V1ClientMessage;
import com.faforever.client.relay.GpgClientMessage;
import com.faforever.client.relay.GpgClientMessageSerializer;
import com.faforever.client.relay.GpgServerMessageType;
import com.faforever.client.remote.domain.AddFoeMessage;
import com.faforever.client.remote.domain.AddFriendMessage;
import com.faforever.client.remote.domain.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.AvatarMessage;
import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.HostGameMessage;
import com.faforever.client.remote.domain.InitSessionMessage;
import com.faforever.client.remote.domain.JoinGameMessage;
import com.faforever.client.remote.domain.ListPersonalAvatarsMessage;
import com.faforever.client.remote.domain.LoginClientMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.remote.domain.Ranked1v1SearchExpansionMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.RemoveFoeMessage;
import com.faforever.client.remote.domain.RemoveFriendMessage;
import com.faforever.client.remote.domain.SelectAvatarMessage;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.ServerCommand;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.remote.domain.SessionMessage;
import com.faforever.client.remote.domain.StatisticsType;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.ConnectivityStateTypeAdapter;
import com.faforever.client.remote.gson.GameAccessTypeAdapter;
import com.faforever.client.remote.gson.GameStateTypeAdapter;
import com.faforever.client.remote.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.InetSocketAddressTypeAdapter;
import com.faforever.client.remote.gson.InitConnectivityTestMessage;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.faforever.client.remote.gson.RatingRangeTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter;
import com.faforever.client.update.ClientUpdateService;
import com.github.nocatch.NoCatch;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class FafServerAccessorImpl extends AbstractServerAccessor implements FafServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long RECONNECT_DELAY = 3000;
  private final Gson gson;
  private final HashMap<Class<? extends ServerMessage>, Collection<Consumer<ServerMessage>>> messageListeners;

  @Resource
  PreferencesService preferencesService;
  @Resource
  UidService uidService;
  @Resource
  ClientUpdateService clientUpdateService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;

  @Value("${lobby.host}")
  String lobbyHost;
  @Value("${lobby.port}")
  int lobbyPort;

  private Task<Void> fafConnectionTask;
  private String localIp;
  private ServerWriter serverWriter;
  private CompletableFuture<LoginMessage> loginFuture;
  private CompletableFuture<SessionMessage> sessionFuture;
  private CompletableFuture<GameLaunchMessage> gameLaunchFuture;
  private ObjectProperty<Long> sessionId;
  private StringProperty login;
  private String username;
  private String password;
  private ObjectProperty<ConnectionState> connectionState;
  private Socket fafServerSocket;
  private CompletableFuture<List<Avatar>> avatarsFuture;

  public FafServerAccessorImpl() {
    messageListeners = new HashMap<>();
    connectionState = new SimpleObjectProperty<>();
    sessionId = new SimpleObjectProperty<>();
    login = new SimpleStringProperty();
    // TODO note to myself; seriously, create a single gson instance (or builder) and put it all there
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameState.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(StatisticsType.class, StatisticsTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(GpgServerMessageType.class, GpgServerMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .registerTypeAdapter(ServerMessage.class, ServerMessageTypeAdapter.INSTANCE)
        .registerTypeAdapter(ConnectivityState.class, ConnectivityStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(InetSocketAddress.class, InetSocketAddressTypeAdapter.INSTANCE)
        .registerTypeAdapter(RatingRange.class, RatingRangeTypeAdapter.INSTANCE)
        .create();

    addOnMessageListener(NoticeMessage.class, this::onNotice);
    addOnMessageListener(SessionMessage.class, this::onSessionInitiated);
    addOnMessageListener(LoginMessage.class, this::onFafLoginSucceeded);
    addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchInfo);
    addOnMessageListener(AuthenticationFailedMessage.class, this::dispatchAuthenticationFailed);
    addOnMessageListener(AvatarMessage.class, this::onAvatarMessage);
  }

  private void onAvatarMessage(AvatarMessage avatarMessage) {
    avatarsFuture.complete(avatarMessage.getAvatarList());
  }

  private void onNotice(NoticeMessage noticeMessage) {
    if (noticeMessage.getText() == null) {
      return;
    }
    notificationService.addNotification(new ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.getText(), noticeMessage.getSeverity()));
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
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletionStage<LoginMessage> connectAndLogIn(String username, String password) {
    sessionFuture = new CompletableFuture<>();
    loginFuture = new CompletableFuture<>();
    this.username = username;
    this.password = password;

    // TODO extract class?
    fafConnectionTask = new Task<Void>() {

      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          logger.info("Trying to connect to FAF server at {}:{}", lobbyHost, lobbyPort);
          connectionState.set(ConnectionState.CONNECTING);

          try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {
            FafServerAccessorImpl.this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            String version = clientUpdateService.getCurrentVersion().toString();
            writeToServer(new InitSessionMessage(version));

            logger.info("FAF server connection established");
            connectionState.set(ConnectionState.CONNECTED);

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            connectionState.set(ConnectionState.DISCONNECTED);
            if (isCancelled()) {
              logger.debug("Connection to FAF server has been closed");
            } else {
              logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
              Thread.sleep(RECONNECT_DELAY);
            }
          }
        }
        return null;
      }

      @Override
      protected void cancelled() {
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(fafServerSocket);
        logger.debug("Closed connection to FAF lobby server");
      }
    };
    executeInBackground(fafConnectionTask);
    return loginFuture;
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo, @Nullable InetSocketAddress relayAddress, int externalPort) {
    HostGameMessage hostGameMessage = new HostGameMessage(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        externalPort,
        new boolean[0],
        newGameInfo.getGameType(),
        newGameInfo.getPassword(),
        null,
        relayAddress
    );

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(hostGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestJoinGame(int gameId, String password, @Nullable InetSocketAddress relayAddress, int externalPort) {
    JoinGameMessage joinGameMessage = new JoinGameMessage(
        gameId,
        externalPort,
        password,
        relayAddress);

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(joinGameMessage);
    return gameLaunchFuture;
  }

  @Override
  @PreDestroy
  public void disconnect() {
    if (fafConnectionTask != null) {
      fafConnectionTask.cancel(true);
    }
  }

  @Override
  public void reconnect() {
    IOUtils.closeQuietly(fafServerSocket);
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
  public CompletionStage<GameLaunchMessage> startSearchRanked1v1(Faction faction, int gamePort, @Nullable InetSocketAddress relayAddress) {
    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(new SearchRanked1V1ClientMessage(gamePort, faction, relayAddress));
    return gameLaunchFuture;
  }

  @Override
  public void stopSearchingRanked() {
    writeToServer(new StopSearchRanked1V1ClientMessage());
    gameLaunchFuture = null;
  }

  @Override
  public void expand1v1Search(float radius) {
    writeToServer(new Ranked1v1SearchExpansionMessage(radius));
  }

  @Override
  public Long getSessionId() {
    return sessionId.get();
  }

  @Override
  public void sendGpgMessage(GpgClientMessage message) {
    writeToServer(message);
  }

  @Override
  public void initConnectivityTest(int port) {
    writeToServer(new InitConnectivityTestMessage(port));
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
  @Cacheable(CacheNames.AVAILABLE_AVATARS)
  public List<Avatar> getAvailableAvatars() {
    avatarsFuture = new CompletableFuture<>();
    writeToServer(new ListPersonalAvatarsMessage());
    return NoCatch.noCatch(() -> avatarsFuture.get(10, TimeUnit.SECONDS));
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(login, sessionId), ClientMessage.class);
    serverWriter.registerMessageSerializer(new PongMessageSerializer(login, sessionId), PongMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    serverWriter.registerMessageSerializer(new GpgClientMessageSerializer(), GpgClientMessage.class);
    return serverWriter;
  }

  private void writeToServer(SerializableMessage message) {
    serverWriter.write(message);
  }

  public void onServerMessage(String message) throws IOException {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      dispatchServerMessage(serverCommand);
    } else {
      parseServerObject(message);
    }
  }

  private void dispatchServerMessage(ServerCommand serverCommand) throws IOException {
    switch (serverCommand) {
      case PING:
        logger.debug("Server PINGed");
        onServerPing();
        break;

      default:
        logger.warn("Unknown server response: {}", serverCommand);
    }
  }

  private void parseServerObject(String jsonString) {
    try {
      ServerMessage serverMessage = gson.fromJson(jsonString, ServerMessage.class);

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
      logger.warn("Could not deserialize message: " + jsonString, e);
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
    logger.info("FAF login succeeded");

    if (loginFuture != null) {
      loginFuture.complete(loginServerMessage);
      loginFuture = null;
    }
  }

  private void onSessionInitiated(SessionMessage sessionMessage) {
    logger.info("FAF session initiated, session ID: {}", sessionMessage.getSession());
    this.sessionId.set(sessionMessage.getSession());
    sessionFuture.complete(sessionMessage);
    logIn(username, password);
  }

  private CompletableFuture<LoginMessage> logIn(String username, String password) {
    String uniqueId = uidService.generate(String.valueOf(sessionId.get()), preferencesService.getFafDataDirectory().resolve("uid.log"));
    writeToServer(new LoginClientMessage(username, password, sessionId.get(), uniqueId, localIp));

    return loginFuture;
  }

  private void onGameLaunchInfo(GameLaunchMessage gameLaunchMessage) {
    gameLaunchFuture.complete(gameLaunchMessage);
    gameLaunchFuture = null;
  }
}
