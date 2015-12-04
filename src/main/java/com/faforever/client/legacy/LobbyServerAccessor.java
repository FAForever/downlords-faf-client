package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.LoginLobbyServerMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import com.faforever.client.relay.GpgClientMessage;
import com.faforever.client.relay.GpgServerMessage;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Entry class for all communication with the FAF lobby server, be it reading or writing. This class should only be
 * called from within services.
 */
public interface LobbyServerAccessor {

  /**
   * Connects to the FAF server and logs in using the credentials from {@link PreferencesService}.
   */
  CompletableFuture<LoginLobbyServerMessage> connectAndLogIn(String username, String password);

  void addOnUpdatedAchievementsInfoListener(Consumer<UpdatedAchievementsMessage> listener);

  void addOnGameTypeInfoListener(OnGameTypeInfoListener listener);

  void addOnGameInfoListener(OnGameInfoListener listener);

  void addOnLoggedInListener(Consumer<LoginLobbyServerMessage> listener);

  void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener);

  CompletionStage<GameLaunchMessage> requestNewGame(NewGameInfo newGameInfo);

  CompletionStage<GameLaunchMessage> requestJoinGame(GameInfoBean gameInfoBean, String password);

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

  CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int gamePort);

  void stopSearchingRanked();

  void expand1v1Search(float radius);

  @Nullable
  Long getSessionId();

  void sendGpgMessage(GpgClientMessage message);

  void initConnectivityTest(int port);

  void addOnGameMessageListener(Consumer<GpgServerMessage> listener);

  void removeOnGameMessageListener(Consumer<GpgServerMessage> listener);

  void addOnConnectivityMessageListener(Consumer<GpgServerMessage> listener);

  void removeOnConnectivityMessageListener(Consumer<GpgServerMessage> listener);

}
