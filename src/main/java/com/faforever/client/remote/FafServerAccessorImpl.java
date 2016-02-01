package com.faforever.client.remote;

import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.AbstractServerAccessor;
import com.faforever.client.legacy.ClientMessageSerializer;
import com.faforever.client.legacy.PongMessage;
import com.faforever.client.legacy.PongMessageSerializer;
import com.faforever.client.legacy.ServerMessageTypeAdapter;
import com.faforever.client.legacy.StringSerializer;
import com.faforever.client.legacy.UidService;
import com.faforever.client.legacy.domain.AddFoeMessage;
import com.faforever.client.legacy.domain.AddFriendMessage;
import com.faforever.client.legacy.domain.AuthenticationFailedMessage;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.HostGameMessage;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.JoinGameMessage;
import com.faforever.client.legacy.domain.LoginClientMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.Ranked1v1SearchExpansionMessage;
import com.faforever.client.legacy.domain.RemoveFoeMessage;
import com.faforever.client.legacy.domain.RemoveFriendMessage;
import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.SessionMessage;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.ConnectivityStateTypeAdapter;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.GpgServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.InitConnectivityTestMessage;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.SearchRanked1V1ClientMessage;
import com.faforever.client.rankedmatch.StopSearchRanked1V1ClientMessage;
import com.faforever.client.relay.GpgClientMessage;
import com.faforever.client.relay.GpgClientMessageSerializer;
import com.faforever.client.relay.GpgServerMessageType;
import com.faforever.client.update.ClientUpdateService;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
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
        .create();

    addOnMessageListener(SessionMessage.class, this::onSessionInitiated);
    addOnMessageListener(LoginMessage.class, this::onFafLoginSucceeded);
    addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchInfo);
    addOnMessageListener(AuthenticationFailedMessage.class, this::dispatchAuthenticationFailed);
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
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    sessionFuture = new CompletableFuture<>();
    loginFuture = new CompletableFuture<>();
    this.username = username;
    this.password = password;

    fafConnectionTask = new Task<Void>() {
      Socket fafServerSocket;

      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          logger.info("Trying to connect to FAF server at {}:{}", lobbyHost, lobbyPort);
          connectionState.set(ConnectionState.CONNECTING);

          try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {
            this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            writeToServer(new InitSessionMessage());

            logger.info("FAF server connection established");
            connectionState.set(ConnectionState.CONNECTED);

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            if (isCancelled()) {
              logger.debug("Connection to FAF server has been closed");
              connectionState.set(ConnectionState.DISCONNECTED);
            } else {
              logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
              connectionState.set(ConnectionState.DISCONNECTED);
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
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo, @Nullable InetSocketAddress relayAddress, int externalPort) {
    HostGameMessage hostGameMessage = new HostGameMessage(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        externalPort,
        new boolean[0],
        newGameInfo.getGameType(),
        newGameInfo.getPassword(),
        newGameInfo.getVersion(),
        relayAddress
    );

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(hostGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password, @Nullable InetSocketAddress relayAddress, int externalPort) {
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
  public void addFriend(int playerId) {
    writeToServer(new AddFriendMessage(playerId));
  }

  @Override
  public void addFoe(int playerId) {
    writeToServer(new AddFoeMessage(playerId));
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int gamePort) {
    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(new SearchRanked1V1ClientMessage(gamePort, faction));
    return gameLaunchFuture;
  }

  @Override
  public void stopSearchingRanked() {
    writeToServer(new StopSearchRanked1V1ClientMessage());
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
  public CompletableFuture<GameLaunchMessage> expectRehostCommand() {
    logger.debug("Expecting rehost command from server");
    gameLaunchFuture = new CompletableFuture<>();
    return gameLaunchFuture;
  }

  @Override
  public void removeFriend(int playerId) {
    writeToServer(new RemoveFriendMessage(playerId));
  }

  @Override
  public void removeFoe(int playerId) {
    writeToServer(new RemoveFoeMessage(playerId));
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
    String version = clientUpdateService.getCurrentVersion().toString();
    writeToServer(new LoginClientMessage(username, password, sessionId.get(), uniqueId, localIp, version));

    return loginFuture;
  }

  private void onGameLaunchInfo(GameLaunchMessage gameLaunchMessage) {
    gameLaunchFuture.complete(gameLaunchMessage);
    gameLaunchFuture = null;
  }
}
