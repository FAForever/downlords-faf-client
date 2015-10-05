package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.domain.GameLaunchInfo;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;

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

  CompletableFuture<List<LeaderboardEntryBean>> requestLeaderboardEntries();

  void addOnJoinChannelsRequestListener(OnJoinChannelsRequestListener listener);

  void setFriends(Collection<String> friends);

  void setFoes(Collection<String> foes);

  void addOnGameLaunchListener(OnGameLaunchInfoListener listener);

  void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener);

  CompletableFuture<GameLaunchInfo> startSearchRanked1v1(Faction faction, int gamePort);

  void stopSearchingRanked();

  void expand1v1Search(float radius);

  /**
   * Returns the 100 most liked mods.
   */
  CompletableFuture<List<ModInfo>> requestMods();
}
