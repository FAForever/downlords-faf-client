package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LadderEntryBean;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockServerAccessor implements ServerAccessor {

  private Collection<OnModInfoListener> onModInfoMessageListeners;
  private OnPlayerInfoListener onPlayerInfoListener;

  private Collection<OnGameInfoListener> onGameInfoListeners;

  @Autowired
  private UserService userService;

  @Autowired
  private TaskService taskService;

  public MockServerAccessor() {
    onModInfoMessageListeners = new ArrayList<>();
    onGameInfoListeners = new ArrayList<>();
  }

  @Override
  public void connectAndLogInInBackground(Callback<Void> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<Void>() {
      @Override
      protected Void call() throws Exception {
        for (OnModInfoListener onModInfoMessageListener : onModInfoMessageListeners) {
          ModInfo modInfo = new ModInfo();
          modInfo.fullname = "Forged Alliance Forever";
          modInfo.name = "faf";
          modInfo.live = true;
          modInfo.host = true;

          onModInfoMessageListener.onModInfo(modInfo);
        }

        if (onPlayerInfoListener != null) {
          PlayerInfo playerInfo = new PlayerInfo();
          playerInfo.login = userService.getUsername();
          playerInfo.clan = "ABC";
          playerInfo.country = "CH";
          onPlayerInfoListener.onPlayerInfo(playerInfo);
        }

        for (OnGameInfoListener onGameInfoListener : onGameInfoListeners) {
          GameInfo gameInfo = new GameInfo();
          gameInfo.title = "Mock game";
          gameInfo.access = "public";
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
        }
        return null;
      }
    }, callback);
  }

  @Override
  public void addOnModInfoMessageListener(OnModInfoListener listener) {
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
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(HIGH) {
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
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<GameLaunchInfo>(HIGH) {
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
  public void setOnLobbyConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener) {

  }

  @Override
  public void setOnLobbyDisconnectedListener(OnLobbyDisconnectedListener onLobbyDisconnectedListener) {

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
}
