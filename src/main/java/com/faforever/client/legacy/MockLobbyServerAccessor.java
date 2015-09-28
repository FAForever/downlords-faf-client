package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameType;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.rankedmatch.RankedMatchNotification;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.legacy.domain.GameAccess.PASSWORD;
import static com.faforever.client.legacy.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;

public class MockLobbyServerAccessor implements LobbyServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Timer timer;

  @Autowired
  UserService userService;
  @Autowired
  TaskService taskService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;

  private Collection<OnGameTypeInfoListener> onModInfoMessageListeners;
  private OnPlayerInfoListener onPlayerInfoListener;
  private Collection<OnGameInfoListener> onGameInfoListeners;
  private Collection<OnRankedMatchNotificationListener> onRankedMatchNotificationListeners;

  public MockLobbyServerAccessor() {
    onModInfoMessageListeners = new ArrayList<>();
    onRankedMatchNotificationListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
    timer = new Timer("LobbyServerAccessorTimer", true);
  }

  @Override
  public CompletableFuture<SessionInfo> connectAndLogInInBackground() {
    return taskService.submitTask(new AbstractPrioritizedTask<SessionInfo>(HIGH) {
      @Override
      protected SessionInfo call() throws Exception {
        updateTitle(i18n.get("login.progress.message"));

        for (OnGameTypeInfoListener onModInfoMessageListener : onModInfoMessageListeners) {
          GameTypeInfo gameTypeInfo = new GameTypeInfo();
          gameTypeInfo.setFullname("Forged Alliance Forever");
          gameTypeInfo.setName("faf");
          gameTypeInfo.setLive(true);
          gameTypeInfo.setHost(true);
          gameTypeInfo.setDesc("Description");

          onModInfoMessageListener.onGameTypeInfo(gameTypeInfo);
        }

        if (onPlayerInfoListener != null) {
          PlayerInfo playerInfo = new PlayerInfo();
          playerInfo.setLogin(userService.getUsername());
          playerInfo.setClan("ABC");
          playerInfo.setCountry("A1");
          playerInfo.setRatingMean(1500);
          playerInfo.setRatingDeviation(220);
          playerInfo.setLadderRatingMean(1500);
          playerInfo.setLadderRatingDeviation(220);
          onPlayerInfoListener.onPlayerInfo(playerInfo);
        }

        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            RankedMatchNotification rankedMatchNotification = new RankedMatchNotification(true);
            onRankedMatchNotificationListeners.forEach(listener -> listener.onRankedMatchInfo(rankedMatchNotification));
          }
        }, 7000);


        for (OnGameInfoListener onGameInfoListener : onGameInfoListeners) {
          onGameInfoListener.onGameInfo(createGameInfo(1, "Mock game 500 - 800", PUBLIC, "faf", "scmp_010", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(2, "Mock game 500+", PUBLIC, "faf", "scmp_011", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(3, "Mock game +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(4, "Mock game <1000", PUBLIC, "faf", "scmp_013", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(5, "Mock game >1000", PUBLIC, "faf", "scmp_014", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(6, "Mock game ~600", PASSWORD, "faf", "scmp_015", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 3, 6, "Mock user"));
        }

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event ->
                        taskService.submitTask(new AbstractPrioritizedTask<Void>(HIGH) {
                          @Override
                          protected Void call() throws Exception {
                            updateTitle("Mock task");
                            Thread.sleep(2000);
                            for (int i = 0; i < 5; i++) {
                              updateProgress(i, 5);
                              Thread.sleep(1000);
                            }
                            return null;
                          }
                        })),
                    new Action("Nope")
                )
            )
        );

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setId(1234);
        sessionInfo.setSession("5678");
        sessionInfo.setEmail("junit@example.com");

        return sessionInfo;
      }
    });
  }

  private GameInfo createGameInfo(int uid, String title, GameAccess access, String featuredMod, String mapName, int numPlayers, int maxPlayers, String host) {
    GameInfo gameInfo = new GameInfo();
    gameInfo.setUid(uid);
    gameInfo.setTitle(title);
    gameInfo.setAccess(access);
    gameInfo.setFeaturedMod(featuredMod);
    gameInfo.setMapname(mapName);
    gameInfo.setNumPlayers(numPlayers);
    gameInfo.setMaxPlayers(maxPlayers);
    gameInfo.setHost(host);
    gameInfo.setState(GameState.OPEN);
    gameInfo.setOptions(new Boolean[0]);
    gameInfo.setSimMods(Collections.emptyMap());
    gameInfo.setTeams(Collections.emptyMap());
    gameInfo.setFeaturedModVersions(Collections.emptyMap());

    return gameInfo;
  }

  @Override
  public void addOnGameTypeInfoListener(OnGameTypeInfoListener listener) {
    onModInfoMessageListeners.add(listener);
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
    return taskService.submitTask(new AbstractPrioritizedTask<GameLaunchInfo>(HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        updateTitle(i18n.get("requestNewGameTask.title"));

        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchInfo.setMod("faf");
        gameLaunchInfo.setUid(1234);
        return gameLaunchInfo;
      }
    });
  }

  @Override
  public CompletionStage<GameLaunchInfo> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    return taskService.submitTask(new AbstractPrioritizedTask<GameLaunchInfo>(HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        updateTitle(i18n.get("requestJoinGameTask.title"));

        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchInfo.setMod("faf");
        gameLaunchInfo.setUid(1234);
        return gameLaunchInfo;
      }
    });
  }

  @Override
  public void notifyGameStarted() {

  }

  @Override
  public void notifyGameTerminated() {

  }

  @Override
  public void setOnFafConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener) {

  }

  @Override
  public void setOnFafDisconnectedListener(OnFafDisconnectedListener onFafDisconnectedListener) {

  }

  @Override
  public void setOnFriendListListener(OnFriendListListener onFriendListListener) {

  }

  @Override
  public void setOnFoeListListener(OnFoeListListener onFoeListListener) {

  }

  @Override
  public void disconnect() {

  }

  @Override
  public void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener) {

  }

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> requestLeaderboardEntries() {

    return null;
  }

  @Override
  public void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener) {

  }

  @Override
  public void setFriends(Collection<String> friends) {

  }

  @Override
  public void setFoes(Collection<String> foes) {

  }

  @Override
  public void addOnGameLaunchListener(OnGameLaunchInfoListener listener) {

  }

  @Override
  public void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener) {
    onRankedMatchNotificationListeners.add(listener);
  }

  @Override
  public CompletableFuture<GameLaunchInfo> startSearchRanked1v1(Faction faction, int gamePort) {
    logger.debug("Searching 1v1 match with faction: {}", faction);
    GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
    gameLaunchInfo.setUid(123);
    gameLaunchInfo.setMod(GameType.DEFAULT.getString());
    return CompletableFuture.completedFuture(gameLaunchInfo);
  }

  @Override
  public void stopSearchingRanked() {
    logger.debug("Stopping searching 1v1 match");
  }

  @Override
  public void expand1v1Search(float radius) {
  }
}
