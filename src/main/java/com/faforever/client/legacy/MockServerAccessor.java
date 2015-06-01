package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MockServerAccessor implements ServerAccessor {

  private Collection<OnModInfoListener> onModInfoMessageListeners;

  public MockServerAccessor() {
    onModInfoMessageListeners = new ArrayList<>();
  }

  @Override
  public void connectAndLogInInBackground(Callback<Void> callback) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
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

        return null;
      }
    }, callback);
  }

  @Override
  public void addOnModInfoMessageListener(OnModInfoListener listener) {
    onModInfoMessageListeners.add(listener);
  }

  @Override
  public void addOnGameInfoMessageListener(OnGameInfoListener listener) {

  }

  @Override
  public void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener) {

  }

  @Override
  public void requestNewGame(NewGameInfo newGameInfo, Callback<GameLaunchInfo> callback) {
    ConcurrentUtil.executeInBackground(new Task<GameLaunchInfo>() {
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
    ConcurrentUtil.executeInBackground(new Task<GameLaunchInfo>() {
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
}
