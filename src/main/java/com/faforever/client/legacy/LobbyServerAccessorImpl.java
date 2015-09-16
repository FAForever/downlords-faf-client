package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.leaderboard.LeaderboardParser;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.FoesMessage;
import com.faforever.client.legacy.domain.FriendsMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameStatusMessage;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.HostGameMessage;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.JoinGameMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.legacy.domain.SocialInfo;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.legacy.domain.GameStatusMessage.Status.OFF;
import static com.faforever.client.legacy.domain.GameStatusMessage.Status.ON;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LobbyServerAccessorImpl extends AbstractServerAccessor implements LobbyServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int VERSION = 0;
  private static final long RECONNECT_DELAY = 3000;
  private final Gson gson;
  private final StringProperty sessionId;
  private final Collection<OnGameInfoListener> onGameInfoListeners;
  private final Collection<OnGameTypeInfoListener> onGameTypeInfoListeners;
  private final Collection<OnJoinChannelsRequestListener> onJoinChannelsRequestListeners;
  private final Collection<OnGameLaunchInfoListener> onGameLaunchListeners;
  @Autowired
  Environment environment;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  LeaderboardParser leaderboardParser;
  @Autowired
  TaskService taskService;
  @Autowired
  I18n i18n;
  @Autowired
  UidService uidService;
  private Task<Void> fafConnectionTask;
  private String username;
  private String password;
  private String localIp;
  private ServerWriter serverWriter;
  private Callback<SessionInfo> loginCallback;
  private CompletableFuture<GameLaunchInfo> gameLaunchFuture;
  // Yes I know, those aren't lists. They will become if it's necessary
  private OnLobbyConnectingListener onLobbyConnectingListener;
  private OnFafDisconnectedListener onFafDisconnectedListener;
  private OnLobbyConnectedListener onLobbyConnectedListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnFriendListListener onFriendListListener;
  private OnFoeListListener onFoeListListener;

  public LobbyServerAccessorImpl() {
    onGameInfoListeners = new ArrayList<>();
    onGameTypeInfoListeners = new ArrayList<>();
    onJoinChannelsRequestListeners = new ArrayList<>();
    onGameLaunchListeners = new ArrayList<>();
    sessionId = new SimpleStringProperty();
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(VictoryCondition.class, new VictoryConditionTypeAdapter())
        .registerTypeAdapter(GameState.class, new GameStateTypeAdapter())
        .registerTypeAdapter(GameAccess.class, new GameAccessTypeAdapter())
        .registerTypeAdapter(ClientMessageType.class, new ClientMessageTypeTypeAdapter())
        .registerTypeAdapter(StatisticsType.class, new StatisticsTypeTypeAdapter())
        .registerTypeAdapter(ServerMessageType.class, new ServerMessageTypeTypeAdapter())
        .create();
  }

  @Override
  public void connectAndLogInInBackground(Callback<SessionInfo> callback) {
    loginCallback = callback;

    LoginPrefs login = preferencesService.getPreferences().getLogin();
    username = login.getUsername();
    password = login.getPassword();

    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      throw new IllegalStateException("Username or password has not been set");
    }

    fafConnectionTask = new Task<Void>() {
      Socket fafServerSocket;

      @Override
      protected Void call() throws Exception {
        while (!isCancelled()) {
          String lobbyHost = environment.getProperty("lobby.host");
          Integer lobbyPort = environment.getProperty("lobby.port", int.class);

          logger.info("Trying to connect to FAF server at {}:{}", lobbyHost, lobbyPort);
          if (onLobbyConnectingListener != null) {
            Platform.runLater(onLobbyConnectingListener::onFaConnecting);
          }

          try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {
            this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            logger.info("FAF server connection established");
            if (onLobbyConnectedListener != null) {
              Platform.runLater(onLobbyConnectedListener::onFaConnected);
            }

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            writeToServer(new InitSessionMessage());

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            if (isCancelled()) {
              logger.debug("Login has been cancelled");
            } else {
              logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
              if (onFafDisconnectedListener != null) {
                Platform.runLater(onFafDisconnectedListener::onFafDisconnected);
              }
              Thread.sleep(RECONNECT_DELAY);
            }
          }
        }
        return null;
      }

      @Override
      protected void cancelled() {
        try {
          if (fafServerSocket != null) {
            serverWriter.close();
            fafServerSocket.close();
          }
          logger.debug("Closed connection to FAF lobby server");
        } catch (IOException e) {
          logger.warn("Could not close fafServerSocket", e);
        }
      }
    };
    executeInBackground(fafConnectionTask);
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(username, sessionId), ClientMessage.class);
    serverWriter.registerMessageSerializer(new PongMessageSerializer(username, sessionId), PongMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    return serverWriter;
  }

  private void writeToServer(Object object) {
    serverWriter.write(object);
  }

  @Override
  public void addOnGameTypeInfoListener(OnGameTypeInfoListener listener) {
    onGameTypeInfoListeners.add(listener);
  }

  @Override
  public void addOnGameInfoListener(OnGameInfoListener listener) {
    onGameInfoListeners.add(listener);
  }

  @Override
  public void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener) {
    onPlayerInfoListener = listener;
  }

  @Override
  public CompletionStage<GameLaunchInfo> requestNewGame(NewGameInfo newGameInfo) {
    HostGameMessage hostGameMessage = new HostGameMessage(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        preferencesService.getPreferences().getForgedAlliance().getPort(),
        new boolean[0],
        newGameInfo.getGameType(),
        newGameInfo.getPassword(),
        newGameInfo.getVersion()
    );

    gameLaunchFuture = new CompletableFuture<>();
    writeToServerInBackground(hostGameMessage);
    return gameLaunchFuture;
  }

  private void writeToServerInBackground(final ClientMessage clientMessage) {
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        writeToServer(clientMessage);
        return null;
      }
    });
  }

  @Override
  public CompletionStage<GameLaunchInfo> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    JoinGameMessage joinGameMessage = new JoinGameMessage(
        gameInfoBean.getUid(),
        preferencesService.getPreferences().getForgedAlliance().getPort(),
        password);

    gameLaunchFuture = new CompletableFuture<>();
    writeToServerInBackground(joinGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public void notifyGameStarted() {
    writeToServer(new GameStatusMessage(ON));
  }

  @Override
  public void notifyGameTerminated() {
    writeToServer(new GameStatusMessage(OFF));
  }

  @Override
  public void setOnFafConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener) {
    this.onLobbyConnectingListener = onLobbyConnectingListener;
  }

  @Override
  public void setOnFafDisconnectedListener(OnFafDisconnectedListener onFafDisconnectedListener) {
    this.onFafDisconnectedListener = onFafDisconnectedListener;
  }

  @Override
  public void setOnFriendListListener(OnFriendListListener onFriendListListener) {
    this.onFriendListListener = onFriendListListener;
  }

  @Override
  public void setOnFoeListListener(OnFoeListListener onFoeListListener) {
    this.onFoeListListener = onFoeListListener;
  }

  @Override
  public void disconnect() {
    fafConnectionTask.cancel();
  }

  @Override
  public void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener) {
    this.onLobbyConnectedListener = onLobbyConnectedListener;
  }

  @Override
  public void requestLadderInfoInBackground(Callback<List<LeaderboardEntryBean>> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<List<LeaderboardEntryBean>>(i18n.get("readLadderTask.title")) {
      @Override
      protected List<LeaderboardEntryBean> call() throws Exception {
        return leaderboardParser.parseLadder();
      }
    }, callback);
  }

  @Override
  public void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener) {
    onJoinChannelsRequestListeners.add(listener);
  }

  @Override
  public void setFriends(Collection<String> friends) {
    writeToServer(new FriendsMessage(friends));
  }

  @Override
  public void setFoes(Collection<String> foes) {
    writeToServer(new FoesMessage(foes));
  }

  @Override
  public void addOnGameLaunchListener(OnGameLaunchInfoListener listener) {
    onGameLaunchListeners.add(listener);
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

      case LOGIN_AVAILABLE:
        logger.warn("Login available: {}", readNextString());
        break;

      case ACK:
        // Number of bytes acknowledged... as a string... I mean, why not.
        int acknowledgedBytes = Integer.parseInt(readNextString());
        // I really don't care. This is TCP!
        logger.debug("Server acknowledged {} bytes", acknowledgedBytes);
        break;

      case ERROR:
        logger.warn("Unhandled error from server: {}", readNextString());
        break;

      case MESSAGE:
        logger.warn("Unhandled message from server: {}", readNextString());
        break;

      default:
        logger.warn("Unknown server response: {}", serverCommand);
    }
  }

  private void parseServerObject(String jsonString) {
    try {
      ServerMessage serverMessage = gson.fromJson(jsonString, ServerMessage.class);

      ServerMessageType serverMessageType = serverMessage.getServerMessageType();

      if (serverMessageType == null) {
        logger.warn("Unknown server object: " + jsonString);
        return;
      }

      switch (serverMessageType) {
        case WELCOME:
          SessionInfo sessionInfo = gson.fromJson(jsonString, SessionInfo.class);
          if (sessionInfo.getSession() != null) {
            onSessionInitiated(sessionInfo);
          } else if (sessionInfo.getEmail() != null) {
            sessionInfo.setSession(sessionId.get());
            onFafLoginSucceeded(sessionInfo);
          }
          break;

        case GAME_INFO:
          GameInfo gameInfo = gson.fromJson(jsonString, GameInfo.class);
          onGameInfo(gameInfo);
          break;

        case PLAYER_INFO:
          PlayerInfo playerInfo = gson.fromJson(jsonString, PlayerInfo.class);
          onPlayerInfoListener.onPlayerInfo(playerInfo);
          break;

        case GAME_LAUNCH:
          GameLaunchInfo gameLaunchInfo = gson.fromJson(jsonString, GameLaunchInfo.class);
          onGameLaunchInfo(gameLaunchInfo);
          break;

        case GAME_TYPE_INFO:
          GameTypeInfo gameTypeInfo = gson.fromJson(jsonString, GameTypeInfo.class);
          onGameTypeInfo(gameTypeInfo);
          break;

        case TUTORIALS_INFO:
          logger.warn("Tutorials info still unhandled: " + jsonString);
          break;

        case MATCHMAKER_INFO:
          logger.warn("Matchmaker info still unhandled: " + jsonString);
          break;

        case SOCIAL:
          SocialInfo socialInfo = gson.fromJson(jsonString, SocialInfo.class);
          dispatchSocialInfo(socialInfo);
          break;
        default:
          logger.warn("Missing case for server object type: " + serverMessageType);
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + jsonString, e);
    }
  }

  private void onServerPing() {
    writeToServer(new PongMessage());
  }

  private void onSessionInitiated(SessionInfo message) {
    this.sessionId.set(message.getSession());
    String uniqueId = uidService.generate(sessionId.get(), preferencesService.getFafDataDirectory().resolve("uid.log"));

    logger.info("FAF session initiated, session ID: {}", sessionId.get());

    writeToServer(new LoginMessage(username, password, sessionId.get(), uniqueId, localIp, VERSION));
  }

  private void onFafLoginSucceeded(SessionInfo sessionInfo) {
    logger.info("FAF login succeeded");

    Platform.runLater(() -> {
      if (loginCallback != null) {
        loginCallback.success(sessionInfo);
        loginCallback = null;
      }
    });
  }

  private void onGameInfo(GameInfo gameInfo) {
    for (OnGameInfoListener listener : onGameInfoListeners) {
      Platform.runLater(() -> listener.onGameInfo(gameInfo));
    }
  }

  private void onGameLaunchInfo(GameLaunchInfo gameLaunchInfo) {
    onGameLaunchListeners.forEach(listener -> listener.onGameLaunchInfo(gameLaunchInfo));
    gameLaunchFuture.complete(gameLaunchInfo);
  }

  private void onGameTypeInfo(GameTypeInfo gameTypeInfo) {
    for (OnGameTypeInfoListener listener : onGameTypeInfoListeners) {
      Platform.runLater(() -> listener.onGameTypeInfo(gameTypeInfo));
    }
  }

  private void dispatchSocialInfo(SocialInfo socialInfo) {
    if (socialInfo.getFriends() != null) {
      onFriendListListener.onFriendList(socialInfo.getFriends());
    } else if (socialInfo.getFoes() != null) {
      onFoeListListener.onFoeList(socialInfo.getFoes());
    } else if (socialInfo.getAutoJoin() != null) {
      onJoinChannelsRequestListeners.forEach(listener -> listener.onJoinChannelsRequest(socialInfo.getAutoJoin()));
    }
  }
}
