package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.GameLaunchMessageLobby;
import com.faforever.client.legacy.domain.LoginLobbyServerMessage;
import com.faforever.client.legacy.relay.GpgClientMessage;
import com.faforever.client.legacy.relay.GpgServerMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
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

  void addOnUpdatedAchievementsInfoListener(Consumer<UpdatedAchievementsMessageLobby> listener);

  void addOnGameTypeInfoListener(OnGameTypeInfoListener listener);

  void addOnGameInfoListener(OnGameInfoListener listener);

  void addOnLoggedInListener(Consumer<LoginLobbyServerMessage> listener);

  void setOnPlayerInfoMessageListener(OnPlayerInfoListener listener);

  CompletionStage<GameLaunchMessageLobby> requestNewGame(NewGameInfo newGameInfo);

  CompletionStage<GameLaunchMessageLobby> requestJoinGame(GameInfoBean gameInfoBean, String password);

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

  CompletableFuture<GameLaunchMessageLobby> startSearchRanked1v1(Faction faction, int gamePort);

  void stopSearchingRanked();

  void expand1v1Search(float radius);

  @Nullable
  Long getSessionId();

  void addOnGpgServerMessageListener(Consumer<GpgServerMessage> listener);

  void sendGpgMessage(GpgClientMessage message);

  void initConnectivityTest();

  void removeOnGpgServerMessageListener(Consumer<GpgServerMessage> listener);

  void addOnConnectivityStateMessageListener(Consumer<FafServerMessage> listener);

  void removeOnFafServerMessageListener(Consumer<FafServerMessage> listener);
}
