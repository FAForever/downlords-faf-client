package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.leaderboard.LeaderboardParser;
import com.faforever.client.legacy.domain.AuthenticationFailedMessage;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.FoesMessage;
import com.faforever.client.legacy.domain.FriendsMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.HostGameMessage;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.JoinGameMessage;
import com.faforever.client.legacy.domain.LoginInfo;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.PlayersInfo;
import com.faforever.client.legacy.domain.Ranked1v1SearchExpansionMessage;
import com.faforever.client.legacy.domain.RequestModsMessage;
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
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.rankedmatch.RankedMatchNotification;
import com.faforever.client.rankedmatch.SearchRanked1v1Message;
import com.faforever.client.rankedmatch.StopSearchRanked1v1Message;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.MEDIUM;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LobbyServerAccessorImpl extends AbstractServerAccessor implements LobbyServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String VERSION = "0";
  private static final long RECONNECT_DELAY = 3000;
  private final Gson gson;
  private final Collection<OnGameInfoListener> onGameInfoListeners;
  private final Collection<OnGameTypeInfoListener> onGameTypeInfoListeners;
  private final Collection<OnJoinChannelsRequestListener> onJoinChannelsRequestListeners;
  private final Collection<OnGameLaunchInfoListener> onGameLaunchListeners;
  private final List<Consumer<UpdatedAchievementsInfo>> onUpdatedAchievementsListeners;
  @Resource
  Environment environment;
  @Resource
  PreferencesService preferencesService;
  @Resource
  LeaderboardParser leaderboardParser;
  @Resource
  TaskService taskService;
  @Resource
  I18n i18n;
  @Resource
  UidService uidService;
  private Task<Void> fafConnectionTask;
  private String localIp;
  private ServerWriter serverWriter;
  private CompletableFuture<LoginInfo> loginFuture;
  private CompletableFuture<SessionInfo> sessionFuture;
  private CompletableFuture<GameLaunchInfo> gameLaunchFuture;
  private Collection<OnRankedMatchNotificationListener> onRankedMatchNotificationListeners;
  // Yes I know, those aren't lists. They will become if it's necessary
  private OnLobbyConnectingListener onLobbyConnectingListener;
  private OnFafDisconnectedListener onFafDisconnectedListener;
  private OnLobbyConnectedListener onLobbyConnectedListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnFriendListListener onFriendListListener;
  private OnFoeListListener onFoeListListener;
  private CompletableFuture<List<ModInfo>> modListFuture;
  private Set<ModInfo> collectedModInfos;
  private Collection<Consumer<LoginInfo>> onLoggedInListeners;
  private ObjectProperty<Long> sessionId;
  private StringProperty login;
  private String username;
  private String password;

  public LobbyServerAccessorImpl() {
    onGameInfoListeners = new ArrayList<>();
    onGameTypeInfoListeners = new ArrayList<>();
    onJoinChannelsRequestListeners = new ArrayList<>();
    onGameLaunchListeners = new ArrayList<>();
    onRankedMatchNotificationListeners = new ArrayList<>();
    onUpdatedAchievementsListeners = new ArrayList<>();
    onLoggedInListeners = new ArrayList<>();
    collectedModInfos = new HashSet<>();
    sessionId = new SimpleObjectProperty<>();
    login = new SimpleStringProperty();
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameState.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(StatisticsType.class, StatisticsTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(ServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
        .create();
  }

  @Override
  public CompletableFuture<LoginInfo> connectAndLogIn(String username, String password) {
    sessionFuture = new CompletableFuture<>();
    loginFuture = new CompletableFuture<>();
    this.username = username;
    this.password = password;

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

            localIp = fafServerSocket.getLocalAddress().getHostAddress();

            serverWriter = createServerWriter(outputStream);

            writeToServer(new InitSessionMessage());

            logger.info("FAF server connection established");
            if (onLobbyConnectedListener != null) {
              Platform.runLater(onLobbyConnectedListener::onFaConnected);
            }

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            if (isCancelled()) {
              logger.debug("Connection to FAF server has been closed");
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
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(fafServerSocket);
        logger.debug("Closed connection to FAF lobby server");
      }
    };
    executeInBackground(fafConnectionTask);
    return loginFuture;
  }

  @Override
  public void addOnUpdatedAchievementsInfoListener(Consumer<UpdatedAchievementsInfo> listener) {
    onUpdatedAchievementsListeners.add(listener);
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
  public void addOnLoggedInListener(Consumer<LoginInfo> listener) {
    onLoggedInListeners.add(listener);
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
  @PreDestroy
  public void disconnect() {
    fafConnectionTask.cancel(true);
  }

  @Override
  public void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener) {
    this.onLobbyConnectedListener = onLobbyConnectedListener;
  }

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> requestLeaderboardEntries() {
    return taskService.submitTask(new AbstractPrioritizedTask<List<LeaderboardEntryBean>>(MEDIUM) {
      @Override
      protected List<LeaderboardEntryBean> call() throws Exception {
        updateTitle(i18n.get("readLadderTask.title"));
        // TODO move this to leaderboard service
        return leaderboardParser.parseLeaderboard();
      }
    });
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

  @Override
  public void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener) {
    onRankedMatchNotificationListeners.add(listener);
  }

  @Override
  public CompletableFuture<GameLaunchInfo> startSearchRanked1v1(Faction faction, int gamePort) {
    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(new SearchRanked1v1Message(gamePort, faction));
    return gameLaunchFuture;
  }

  @Override
  public void stopSearchingRanked() {
    writeToServer(new StopSearchRanked1v1Message());
  }

  @Override
  public void expand1v1Search(float radius) {
    writeToServer(new Ranked1v1SearchExpansionMessage(radius));
  }

  @Override
  public CompletableFuture<List<ModInfo>> requestMods() {
    modListFuture = new CompletableFuture<>();
    writeToServer(new RequestModsMessage());
    return modListFuture;
  }

  @Override
  public Long getSessionId() {
    return sessionId.get();
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(login, sessionId), ClientMessage.class);
    serverWriter.registerMessageSerializer(new PongMessageSerializer(login, sessionId), PongMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    return serverWriter;
  }

  private void writeToServer(Object object) {
    serverWriter.write(object);
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
        logger.trace("Server acknowledged {} bytes", acknowledgedBytes);
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
        case SESSION:
          SessionInfo sessionInfo = gson.fromJson(jsonString, SessionInfo.class);
          onSessionInitiated(sessionInfo);
          break;

        case WELCOME:
          LoginInfo loginInfo = gson.fromJson(jsonString, LoginInfo.class);
          onFafLoginSucceeded(loginInfo);
          break;

        case GAME_INFO:
          GameInfo gameInfo = gson.fromJson(jsonString, GameInfo.class);
          onGameInfo(gameInfo);
          break;

        case PLAYER_INFO:
          PlayersInfo player = gson.fromJson(jsonString, PlayersInfo.class);
          player.getPlayers().forEach(onPlayerInfoListener::onPlayerInfo);
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
          RankedMatchNotification rankedMatchNotification = gson.fromJson(jsonString, RankedMatchNotification.class);
          onRankedMatchInfo(rankedMatchNotification);
          break;

        case SOCIAL:
          SocialInfo socialInfo = gson.fromJson(jsonString, SocialInfo.class);
          dispatchSocialInfo(socialInfo);
          break;

        case AUTHENTICATION_FAILED:
          AuthenticationFailedMessage message = gson.fromJson(jsonString, AuthenticationFailedMessage.class);
          dispatchAuthenticationFailed(message);
          break;

        case MOD_VAULT_INFO:
          ModInfo modInfo = gson.fromJson(jsonString, ModInfo.class);
          onModInfo(modInfo);
          break;

        case UPDATED_ACHIEVEMENTS:
          UpdatedAchievementsInfo updatedAchievementsInfo = gson.fromJson(jsonString, UpdatedAchievementsInfo.class);
          onUpdatedAchievementsListeners.forEach(listener -> listener.accept(updatedAchievementsInfo));
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

  private void onSessionInitiated(SessionInfo sessionInfo) {
    logger.info("FAF session initiated, session ID: {}", sessionInfo.getSession());
    this.sessionId.set(sessionInfo.getSession());
    sessionFuture.complete(sessionInfo);
    logIn(username, password);
  }

  private void onFafLoginSucceeded(LoginInfo loginInfo) {
    logger.info("FAF login succeeded");

    if (loginFuture != null) {
      loginFuture.complete(loginInfo);
      loginFuture = null;
    }
  }

  private void onGameInfo(GameInfo gameInfo) {
    onGameInfoListeners.forEach(listener -> listener.onGameInfo(gameInfo));
  }

  private void onGameLaunchInfo(GameLaunchInfo gameLaunchInfo) {
    onGameLaunchListeners.forEach(listener -> listener.onGameLaunchInfo(gameLaunchInfo));
    gameLaunchFuture.complete(gameLaunchInfo);
    gameLaunchFuture = null;
  }

  private void onGameTypeInfo(GameTypeInfo gameTypeInfo) {
    onGameTypeInfoListeners.forEach(listener -> listener.onGameTypeInfo(gameTypeInfo));
  }

  private void onRankedMatchInfo(RankedMatchNotification gameTypeInfo) {
    onRankedMatchNotificationListeners.forEach(listener -> listener.onRankedMatchInfo(gameTypeInfo));
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

  private void dispatchAuthenticationFailed(AuthenticationFailedMessage message) {
    loginFuture.completeExceptionally(new LoginFailedException(message.getText()));
    loginFuture = null;
  }

  /**
   * Instead of sending a list of mod info, the server sends one mod after another. This is very inconvenient since we
   * don't really know how many there will be. However, at the moment, we "know" that the server sends 100 mods. So
   * let's wait for these and pray that this number will never change until we get a proper server API.
   */
  private void onModInfo(ModInfo modInfo) {
    collectedModInfos.add(modInfo);
    if (collectedModInfos.size() == 100) {
      modListFuture.complete(new ArrayList<>(collectedModInfos));
      collectedModInfos.clear();
    }
  }

  private CompletableFuture<LoginInfo> logIn(String username, String password) {
    String uniqueId = uidService.generate(String.valueOf(sessionId.get()), preferencesService.getFafDataDirectory().resolve("uid.log"));
    writeToServer(new LoginMessage(username, password, sessionId.get(), uniqueId, localIp, VERSION));

    return loginFuture;
  }
}
