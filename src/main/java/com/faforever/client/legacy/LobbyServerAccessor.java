package com.faforever.client.legacy;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.preferences.PreferencesService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Entry class for all communication with the FAF lobby server, be it reading or writing. This class should only be
 * called from within services.
 */
public interface LobbyServerAccessor {

  /**
   * Connects to the FAF server and logs in using the credentials from {@link PreferencesService}.
   */
  CompletableFuture<SessionInfo> connectAndLogInInBackground();

  void addOnGameTypeInfoListener(OnGameTypeInfoListener listener);

  void addOnGameInfoListener(OnGameInfoListener listener);

  void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener);

  CompletionStage<GameLaunchInfo> requestNewGame(NewGameInfo newGameInfo);

  CompletionStage<GameLaunchInfo> requestJoinGame(GameInfoBean gameInfoBean, String password);

  void notifyGameStarted();

  void notifyGameTerminated();

  void setOnFafConnectingListener(OnLobbyConnectingListener onLobbyConnectingListener);

  void setOnFafDisconnectedListener(OnFafDisconnectedListener onFafDisconnectedListener);

  void setOnFriendListListener(OnFriendListListener onFriendListListener);

  void setOnFoeListListener(OnFoeListListener onFoeListListener);

  void disconnect();

  void setOnLobbyConnectedListener(OnLobbyConnectedListener onLobbyConnectedListener);

  CompletableFuture<List<LeaderboardEntryBean>> requestLadderInfoInBackground();

  void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener);

  void setFriends(Collection<String> friends);

  void setFoes(Collection<String> foes);

  void addOnGameLaunchListener(OnGameLaunchInfoListener listener);

}
