package com.faforever.client.remote;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.clan.Clan;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// TODO divide and conquer
public interface FafService {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password);

  CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction, int port);

  void stopSearchingRanked();

  void initConnectivityTest(int port);

  void sendGpgGameMessage(GpgGameMessage message);

  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void addFriend(Player friendId);

  void addFoe(Player foeId);

  void removeFriend(Player friendId);

  void removeFoe(Player foeId);

  CompletableFuture<LeaderboardEntry> getLadder1v1EntryForPlayer(int playerId);

  void notifyGameEnded();

  CompletableFuture<List<ModVersion>> getMods();

  CompletableFuture<ModVersion> getModVersion(String uid);

  void reconnect();

  CompletableFuture<List<MapBean>> getMostPlayedMaps(int count, int page);

  CompletableFuture<List<MapBean>> getHighestRatedMaps(int count, int page);

  CompletableFuture<List<MapBean>> getNewestMaps(int count, int page);

  CompletableFuture<List<CoopMission>> getCoopMaps();

  CompletableFuture<List<AvatarBean>> getAvailableAvatars();

  void selectAvatar(AvatarBean avatar);

  void evictModsCache();

  CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers);

  CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, KnownFeaturedMod knownFeaturedMod);

  CompletableFuture<List<FeaturedMod>> getFeaturedMods();

  CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  CompletableFuture<List<LeaderboardEntry>> getLadder1v1Leaderboard();

  CompletableFuture<List<LeaderboardEntry>> getGlobalLeaderboard();

  CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page);

  CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount, int page);

  void uploadMod(Path modFile, ByteCountListener byteListener);

  CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);

  void sendIceMessage(int remotePlayerId, Object message);

  CompletableFuture<List<Replay>> findReplaysByQuery(String condition, int maxResults, int page, SortConfig sortConfig);

  CompletableFuture<List<MapBean>> findMapsByQuery(String query, int page, int maxSearchResults, SortConfig sortConfig);

  CompletableFuture<Optional<MapBean>> findMapByFolderName(String folderName);

  CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds);

  CompletableFuture<Void> saveGameReview(Review review, int gameId);

  CompletableFuture<Void> saveModVersionReview(Review review, String modVersionId);

  CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId);

  CompletableFuture<Optional<Replay>> getLastGameOnMap(int playerId, String mapVersionId);

  CompletableFuture<Void> deleteGameReview(Review review);

  CompletableFuture<Optional<Clan>> getClanByTag(String tag);

  Optional<MapBean> findMapById(String id);

  CompletableFuture<Void> deleteMapVersionReview(Review review);

  CompletableFuture<List<ModVersion>> findModsByQuery(SearchConfig query, int page, int count);

  void setRelayAddress(InetSocketAddress relayAddress);

  void setExternalSocketPort(int externalSocketPort);

  CompletableFuture<Void> deleteModVersionReview(Review review);

  CompletableFuture<Optional<Replay>> findReplayById(int id);

  CompletableFuture<List<IceServer>> getIceServers();

  void restoreGameSession(int id);

  CompletableFuture<List<MapBean>> getLadder1v1Maps(int count, int page);
}
