package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
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
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockLobbyServerAccessor implements LobbyServerAccessor {

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

  public MockLobbyServerAccessor() {
    onModInfoMessageListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
  }

  @Override
  public void connectAndLogInInBackground(Callback<SessionInfo> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<SessionInfo>(i18n.get("login.progress.message")) {
      @Override
      protected SessionInfo call() throws Exception {
        for (OnGameTypeInfoListener onModInfoMessageListener : onModInfoMessageListeners) {
          GameTypeInfo gameTypeInfo = new GameTypeInfo();
          gameTypeInfo.fullname = "Forged Alliance Forever";
          gameTypeInfo.name = "faf";
          gameTypeInfo.live = true;
          gameTypeInfo.host = true;
          gameTypeInfo.desc = "Description";

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
          onGameInfoListener.onGameInfo(createGameInfo(1, "Mock game 1 500 - 800", PUBLIC, "faf", "scmp_010", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(2, "Mock game 2 500+", PUBLIC, "faf", "scmp_011", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(3, "Mock game 3 +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(4, "Mock game 4 <1000", PUBLIC, "faf", "scmp_013", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(5, "Mock game 5 >1000", PUBLIC, "faf", "scmp_014", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(6, "Mock game 6", PUBLIC, "faf", "scmp_015", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(7, "Mock game 7", PUBLIC, "faf", "scmp_016", 3, 6, "Mock user"));
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

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.id = 1234;
        sessionInfo.session = "5678";
        sessionInfo.email = "junit@example.com";

        return sessionInfo;
      }
    }, callback);
  }

  private GameInfo createGameInfo(int uid, String title, GameAccess access, String featuredMod, String mapName, int numPlayers, int maxPlayers, String host) {
    GameInfo gameInfo = new GameInfo();
    gameInfo.uid = uid;
    gameInfo.title = title;
    gameInfo.access = access;
    gameInfo.featuredMod = featuredMod;
    gameInfo.mapname = mapName;
    gameInfo.numPlayers = numPlayers;
    gameInfo.maxPlayers = maxPlayers;
    gameInfo.host = host;
    gameInfo.state = GameState.OPEN;
    gameInfo.options = new Boolean[0];
    gameInfo.simMods = Collections.emptyMap();
    gameInfo.teams = Collections.emptyMap();
    gameInfo.featuredModVersions = Collections.emptyMap();

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
  public void requestLadderInfoInBackground(Callback<List<LeaderboardEntryBean>> callback) {

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
}
