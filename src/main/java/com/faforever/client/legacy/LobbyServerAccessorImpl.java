package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.leaderboard.LeaderboardParser;
import com.faforever.client.legacy.domain.AuthenticationFailedMessageLobby;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.FoesMessage;
import com.faforever.client.legacy.domain.FriendsMessage;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfoMessage;
import com.faforever.client.legacy.domain.GameLaunchMessageLobby;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeMessage;
import com.faforever.client.legacy.domain.HostGameMessage;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.JoinGameMessage;
import com.faforever.client.legacy.domain.LoginClientMessage;
import com.faforever.client.legacy.domain.LoginLobbyServerMessage;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.domain.PlayersMessageLobby;
import com.faforever.client.legacy.domain.Ranked1v1SearchExpansionMessage;
import com.faforever.client.legacy.domain.SerializableMessage;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.SessionMessageLobby;
import com.faforever.client.legacy.domain.SocialMessageLobby;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.InitConnectivityTestMessage;
import com.faforever.client.legacy.gson.MessageTargetTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.faforever.client.legacy.relay.GpgClientMessage;
import com.faforever.client.legacy.relay.GpgClientMessageSerializer;
import com.faforever.client.legacy.relay.GpgServerMessage;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerLobbyServerMessage;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.rankedmatch.SearchRanked1V1ClientMessage;
import com.faforever.client.rankedmatch.StopSearchRanked1V1ClientMessage;
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
import java.util.List;
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
  private final List<Consumer<UpdatedAchievementsMessageLobby>> onUpdatedAchievementsListeners;
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
  private CompletableFuture<LoginLobbyServerMessage> loginFuture;
  private CompletableFuture<SessionMessageLobby> sessionFuture;
  private CompletableFuture<GameLaunchMessageLobby> gameLaunchFuture;
  private Collection<OnRankedMatchNotificationListener> onRankedMatchNotificationListeners;
  // Yes I know, those aren't lists. They will become if it's necessary
  private OnLobbyConnectingListener onLobbyConnectingListener;
  private OnFafDisconnectedListener onFafDisconnectedListener;
  private OnLobbyConnectedListener onLobbyConnectedListener;
  private OnPlayerInfoListener onPlayerInfoListener;
  private OnFriendListListener onFriendListListener;
  private OnFoeListListener onFoeListListener;
  private Collection<Consumer<LoginLobbyServerMessage>> onLoggedInListeners;
  private ObjectProperty<Long> sessionId;
  private StringProperty login;
  private String username;
  private String password;
  private Collection<Consumer<GpgServerMessage>> gpgServerMessageListeners;
  private Collection<Consumer<FafServerMessage>> connectivityStateMessageListener;

  public LobbyServerAccessorImpl() {
    onGameInfoListeners = new ArrayList<>();
    onGameTypeInfoListeners = new ArrayList<>();
    onJoinChannelsRequestListeners = new ArrayList<>();
    onGameLaunchListeners = new ArrayList<>();
    onRankedMatchNotificationListeners = new ArrayList<>();
    onUpdatedAchievementsListeners = new ArrayList<>();
    gpgServerMessageListeners = new ArrayList<>();
    connectivityStateMessageListener = new ArrayList<>();
    onLoggedInListeners = new ArrayList<>();
    sessionId = new SimpleObjectProperty<>();
    login = new SimpleStringProperty();
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(VictoryCondition.class, VictoryConditionTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameState.class, GameStateTypeAdapter.INSTANCE)
        .registerTypeAdapter(GameAccess.class, GameAccessTypeAdapter.INSTANCE)
        .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(StatisticsType.class, StatisticsTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
        .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
        .registerTypeAdapter(ServerMessage.class, ServerMessageTypeAdapter.INSTANCE)
        .create();
  }

  @Override
  public CompletableFuture<LoginLobbyServerMessage> connectAndLogIn(String username, String password) {
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
  public void addOnUpdatedAchievementsInfoListener(Consumer<UpdatedAchievementsMessageLobby> listener) {
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
  public void addOnLoggedInListener(Consumer<LoginLobbyServerMessage> listener) {
    onLoggedInListeners.add(listener);
  }

  @Override
  public void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener) {
    onPlayerInfoListener = listener;
  }

  @Override
  public CompletionStage<GameLaunchMessageLobby> requestNewGame(NewGameInfo newGameInfo) {
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
    writeToServer(hostGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public CompletionStage<GameLaunchMessageLobby> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    JoinGameMessage joinGameMessage = new JoinGameMessage(
        gameInfoBean.getUid(),
        preferencesService.getPreferences().getForgedAlliance().getPort(),
        password);

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(joinGameMessage);
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
  public CompletableFuture<GameLaunchMessageLobby> startSearchRanked1v1(Faction faction, int gamePort) {
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
  public void addOnGpgServerMessageListener(Consumer<GpgServerMessage> listener) {
    gpgServerMessageListeners.add(listener);
  }

  @Override
  public void sendGpgMessage(GpgClientMessage message) {
    writeToServer(message);
  }

  @Override
  public void initConnectivityTest() {
    InitConnectivityTestMessage connectivityTestMessage = new InitConnectivityTestMessage();
    connectivityTestMessage.setPort(preferencesService.getPreferences().getForgedAlliance().getPort());
    writeToServer(connectivityTestMessage);
  }

  @Override
  public void removeOnGpgServerMessageListener(Consumer<GpgServerMessage> listener) {
    gpgServerMessageListeners.remove(listener);
  }

  @Override
  public void addOnConnectivityStateMessageListener(Consumer<FafServerMessage> listener) {
    connectivityStateMessageListener.add(listener);
  }

  @Override
  public void removeOnFafServerMessageListener(Consumer<FafServerMessage> listener) {
    connectivityStateMessageListener.remove(listener);
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
      serverMessage.setJsonString(jsonString);

      switch (serverMessage.getTarget()) {
        case GAME:
          gpgServerMessageListeners.forEach(listener -> listener.accept((GpgServerMessage) serverMessage));
          break;

        case CONNECTIVITY:
        case CLIENT:
          dispatchServerToClientMessage((FafServerMessage) serverMessage);
          break;

        default:
          throw new AssertionError("Uncovered target: " + serverMessage.getTarget());
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + jsonString, e);
    }
  }

  private void onServerPing() {
    writeToServer(new PongMessage());
  }

  private void dispatchServerToClientMessage(FafServerMessage fafServerMessage) {
    switch (fafServerMessage.getMessageType()) {
      case SESSION:
        onSessionInitiated((SessionMessageLobby) fafServerMessage);
        break;

      case WELCOME:
        onFafLoginSucceeded((LoginLobbyServerMessage) fafServerMessage);
        break;

      case GAME_INFO:
        onGameInfo((GameInfoMessage) fafServerMessage);
        break;

      case PLAYER_INFO:
        ((PlayersMessageLobby) fafServerMessage).getPlayers().forEach(onPlayerInfoListener::onPlayerInfo);
        break;

      case GAME_LAUNCH:
        onGameLaunchInfo((GameLaunchMessageLobby) fafServerMessage);
        break;

      case GAME_TYPE_INFO:
        onGameTypeInfo((GameTypeMessage) fafServerMessage);
        break;

      case MATCHMAKER_INFO:
        onRankedMatchInfo((MatchmakerLobbyServerMessage) fafServerMessage);
        break;

      case SOCIAL:
        dispatchSocialInfo((SocialMessageLobby) fafServerMessage);
        break;

      case AUTHENTICATION_FAILED:
        dispatchAuthenticationFailed((AuthenticationFailedMessageLobby) fafServerMessage);
        break;

      case UPDATED_ACHIEVEMENTS:
        onUpdatedAchievementsListeners.forEach(listener -> listener.accept((UpdatedAchievementsMessageLobby) fafServerMessage));
        break;

      case CONNECTIVITY_STATE:
        connectivityStateMessageListener.forEach(listener -> listener.accept(fafServerMessage));
        break;

      default:
        logger.warn("Missing case for server object type: " + fafServerMessage.getMessageType());
    }
  }

  private void onSessionInitiated(SessionMessageLobby sessionMessage) {
    logger.info("FAF session initiated, session ID: {}", sessionMessage.getSession());
    this.sessionId.set(sessionMessage.getSession());
    sessionFuture.complete(sessionMessage);
    logIn(username, password);
  }

  private void onFafLoginSucceeded(LoginLobbyServerMessage loginServerMessage) {
    logger.info("FAF login succeeded");

    if (loginFuture != null) {
      loginFuture.complete(loginServerMessage);
      loginFuture = null;
    }
  }

  private void onGameInfo(GameInfoMessage gameInfoMessage) {
    onGameInfoListeners.forEach(listener -> listener.onGameInfo(gameInfoMessage));
  }

  private void onGameLaunchInfo(GameLaunchMessageLobby gameLaunchMessage) {
    onGameLaunchListeners.forEach(listener -> listener.onGameLaunchInfo(gameLaunchMessage));
    gameLaunchFuture.complete(gameLaunchMessage);
    gameLaunchFuture = null;
  }

  private void onGameTypeInfo(GameTypeMessage gameTypeMessage) {
    onGameTypeInfoListeners.forEach(listener -> listener.onGameTypeInfo(gameTypeMessage));
  }

  private void onRankedMatchInfo(MatchmakerLobbyServerMessage gameTypeInfo) {
    onRankedMatchNotificationListeners.forEach(listener -> listener.onRankedMatchInfo(gameTypeInfo));
  }

  private void dispatchSocialInfo(SocialMessageLobby socialMessage) {
    onFriendListListener.onFriendList(socialMessage.getFriends());
    onFoeListListener.onFoeList(socialMessage.getFoes());
    onJoinChannelsRequestListeners.forEach(listener -> listener.onJoinChannelsRequest(socialMessage.getAutoJoin()));
  }

  private void dispatchAuthenticationFailed(AuthenticationFailedMessageLobby message) {
    loginFuture.completeExceptionally(new LoginFailedException(message.getText()));
    loginFuture = null;
  }

  private CompletableFuture<LoginLobbyServerMessage> logIn(String username, String password) {
    String uniqueId = uidService.generate(String.valueOf(sessionId.get()), preferencesService.getFafDataDirectory().resolve("uid.log"));
    writeToServer(new LoginClientMessage(username, password, sessionId.get(), uniqueId, localIp, VERSION));

    return loginFuture;
  }
}
