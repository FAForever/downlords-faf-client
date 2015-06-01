package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;

/**
 * Entry class for all communication with the FAF lobby server, be it reading or writing. This class should only be
 * called from within services.
 */
public interface ServerAccessor {

  /**
   * Connects to the FAF server and logs in using the credentials from {@link PreferencesService}. This method runs in
   * background, the callback however is called on the FX application thread.
   */
  void connectAndLogInInBackground(Callback<Void> callback);

  void addOnModInfoMessageListener(OnModInfoListener listener);

  void addOnGameInfoMessageListener(OnGameInfoListener listener);

  void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener);

  void requestNewGame(NewGameInfo newGameInfo, Callback<GameLaunchInfo> callback);

  void requestJoinGame(GameInfoBean gameInfoBean, String password, Callback<GameLaunchInfo> callback);

  void notifyGameStarted();

  void notifyGameTerminated();

  void setOnLobbyConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener);

  void setOnLobbyDisconnectedListener(OnLobbyDisconnectedListener onLobbyDisconnectedListener);

  void setOnFriendListListener(OnFriendListListener onFriendListListener);

  void setOnFoeListListener(OnFoeListListener onFoeListListener);

  void disconnect();

  void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener);
}
