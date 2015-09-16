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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.legacy.domain.GameAccess.PASSWORD;
import static com.faforever.client.legacy.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockLobbyServerAccessor implements LobbyServerAccessor {

  private final Collection<OnGameTypeInfoListener> onModInfoMessageListeners;
  private final Collection<OnGameInfoListener> onGameInfoListeners;

  @Autowired
  UserService userService;
  @Autowired
  TaskService taskService;
  @Autowired
  NotificationService notificationService;
  @Autowired
  I18n i18n;

  private OnPlayerInfoListener onPlayerInfoListener;

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
          onPlayerInfoListener.onPlayerInfo(playerInfo);
        }

        for (OnGameInfoListener onGameInfoListener : onGameInfoListeners) {
          onGameInfoListener.onGameInfo(createGameInfo(1, "Mock game 1 500 - 800", PUBLIC, "faf", "scmp_010", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(2, "Mock game 2 500+", PUBLIC, "faf", "scmp_011", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(3, "Mock game 3 +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(4, "Mock game 4 <1000", PUBLIC, "faf", "scmp_013", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(5, "Mock game 5 >1000", PUBLIC, "faf", "scmp_014", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(6, "Mock game 6", PASSWORD, "faf", "scmp_015", 3, 6, "Mock user"));
          onGameInfoListener.onGameInfo(createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 3, 6, "Mock user"));
        }

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event ->
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
    }, callback);
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
    CompletableFuture<GameLaunchInfo> future = new CompletableFuture<>();

    Callback<GameLaunchInfo> callback = new Callback<GameLaunchInfo>() {
      @Override
      public void success(GameLaunchInfo result) {
        future.complete(result);
      }

      @Override
      public void error(Throwable e) {
        future.completeExceptionally(e);
      }
    };

    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(i18n.get("requestNewGameTask.title"), HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchInfo.setMod("faf");
        gameLaunchInfo.setUid(1234);
        return gameLaunchInfo;
      }
    }, callback);
    return future;
  }

  @Override
  public CompletionStage<GameLaunchInfo> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    CompletableFuture<GameLaunchInfo> future = new CompletableFuture<>();

    Callback<GameLaunchInfo> callback = new Callback<GameLaunchInfo>() {
      @Override
      public void success(GameLaunchInfo result) {
        future.complete(result);
      }

      @Override
      public void error(Throwable e) {
        future.completeExceptionally(e);
      }
    };

    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(i18n.get("requestJoinGameTask.title"), HIGH) {
      @Override
      protected GameLaunchInfo call() throws Exception {
        GameLaunchInfo gameLaunchInfo = new GameLaunchInfo();
        gameLaunchInfo.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchInfo.setMod("faf");
        gameLaunchInfo.setUid(1234);
        return gameLaunchInfo;
      }
    }, callback);

    return future;
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
