package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LadderEntryBean;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockServerAccessor implements ServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final int ONE_DAY = 86_400_000;

  private Collection<OnGameTypeInfoListener> onModInfoMessageListeners;
  private OnPlayerInfoListener onPlayerInfoListener;
  private Collection<OnGameInfoListener> onGameInfoListeners;

  @Autowired
  UserService userService;

  @Autowired
  TaskService taskService;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  public MockServerAccessor() {
    onModInfoMessageListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
  }

  @Override
  public void connectAndLogInInBackground(Callback<Void> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<Void>(i18n.get("login.progress.message")) {
      @Override
      protected Void call() throws Exception {
        for (OnGameTypeInfoListener onModInfoMessageListener : onModInfoMessageListeners) {
          GameTypeInfo gameTypeInfo = new GameTypeInfo();
          gameTypeInfo.fullname = "Forged Alliance Forever";
          gameTypeInfo.name = "faf";
          gameTypeInfo.live = true;
          gameTypeInfo.host = true;

          onModInfoMessageListener.onGameTypeInfo(gameTypeInfo);
        }

        if (onPlayerInfoListener != null) {
          PlayerInfo playerInfo = new PlayerInfo();
          playerInfo.login = userService.getUsername();
          playerInfo.clan = "ABC";
          playerInfo.country = "A1";
          onPlayerInfoListener.onPlayerInfo(playerInfo);
        }

        for (OnGameInfoListener onGameInfoListener : onGameInfoListeners) {
          GameInfo gameInfo = new GameInfo();
          gameInfo.uid = 1;
          gameInfo.title = "Mock game";
          gameInfo.access = GameAccess.PUBLIC;
          gameInfo.featuredMod = "faf";
          gameInfo.mapname = "scmp_015";
          gameInfo.numPlayers = 3;
          gameInfo.maxPlayers = 6;
          gameInfo.host = "Mock user";
          gameInfo.state = GameState.OPEN;
          gameInfo.options = new Boolean[0];
          gameInfo.simMods = Collections.emptyMap();
          gameInfo.teams = Collections.emptyMap();
          gameInfo.featuredModVersions = Collections.emptyMap();

          onGameInfoListener.onGameInfo(gameInfo);
          gameInfo = new GameInfo();
          gameInfo.uid = 2;
          gameInfo.title = "Protected mock game";
          gameInfo.access = GameAccess.PASSWORD;
          gameInfo.featuredMod = "faf";
          gameInfo.mapname = "scmp_016";
          gameInfo.numPlayers = 1;
          gameInfo.maxPlayers = 6;
          gameInfo.host = "Mock user";
          gameInfo.state = GameState.OPEN;
          gameInfo.options = new Boolean[0];
          gameInfo.simMods = Collections.emptyMap();
          gameInfo.teams = Collections.emptyMap();
          gameInfo.featuredModVersions = Collections.emptyMap();

          onGameInfoListener.onGameInfo(gameInfo);
        }

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event -> {
                      taskService.submitTask(TaskGroup.NET_HEAVY, new PrioritizedTask<Void>("Mock task") {
                        @Override
                        protected Void call() throws Exception {
                          Thread.sleep(2000);
                          for (int i = 0; i < 5; i++) {
                            updateProgress(i, 5);
                            Thread.sleep(1000);
                          }
                          return null;
                        }
                      });
                    }),
                    new Action("Nope")
                )
            )
        );

        return null;
      }
    }, callback);
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
  public void requestNewGame(NewGameInfo newGameInfo, Callback<GameLaunchInfo> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(i18n.get("requestNewGameTask.title"), HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.args = Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234");
        gameLaunchInfo.mod = "faf";
        gameLaunchInfo.uid = 1234;
        gameLaunchInfo.version = "1";
        return gameLaunchInfo;
      }
    }, callback);
  }

  @Override
  public void requestJoinGame(GameInfoBean gameInfoBean, String password, Callback<GameLaunchInfo> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(i18n.get("requestJoinGameTask.title"), HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.args = Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234");
        gameLaunchInfo.mod = "faf";
        gameLaunchInfo.uid = 1234;
        gameLaunchInfo.version = "1";
        return gameLaunchInfo;
      }
    }, callback);
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
  public void requestLadderInfoInBackground(Callback<List<LadderEntryBean>> callback) {

  }

  @Override
  public void requestPlayerStatistics(String username, Callback<PlayerStatistics> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<PlayerStatistics>("Fetching player statistics") {
      @Override
      protected PlayerStatistics call() throws Exception {
        ArrayList<RatingInfo> ratings = new ArrayList<>();
        for (int day = 0; day < 90; day++) {
          LocalDateTime localDateTime = LocalDateTime.now().plusDays(day);
          float mean = (float) (1500 + Math.sin(day) * 300);
          float dev = 60;
          ratings.add(createRatingInfo(localDateTime, mean, dev));
        }

        PlayerStatistics playerStatistics = new PlayerStatistics();
        playerStatistics.type = StatisticsType.STATS;
        playerStatistics.values = ratings;
        return playerStatistics;
      }
    }, callback);
  }

  private RatingInfo createRatingInfo(LocalDateTime dateTime, float mean, float dev) {
    RatingInfo ratingInfo = new RatingInfo();
    ratingInfo.date = dateTime.toLocalDate();
    ratingInfo.time = dateTime.toLocalTime();
    ratingInfo.mean = mean;
    ratingInfo.dev = dev;
    return ratingInfo;
  }
}
