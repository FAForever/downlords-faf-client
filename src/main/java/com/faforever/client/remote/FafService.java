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
import com.faforever.client.mod.Mod;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import com.faforever.client.vault.review.Review;
import com.faforever.commons.io.ByteCountListener;
import javafx.beans.property.ReadOnlyObjectProperty;

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

  void sendGpgGameMessage(GpgGameMessage message);

  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void addFriend(Player friendId);

  void addFoe(Player foeId);

  void removeFriend(Player friendId);

  void removeFoe(Player foeId);

  CompletableFuture<LeaderboardEntry> getLadder1v1EntryForPlayer(int playerId);

  void notifyGameEnded();

  CompletableFuture<List<Mod>> getMods();

  CompletableFuture<Mod> getMod(String uid);

  void reconnect();

  CompletableFuture<List<MapBean>> getMostPlayedMaps(int count, int page);

  CompletableFuture<List<MapBean>> getMostLikedMaps(int count, int page);

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

  CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount, int page);

  void uploadMod(Path modFile, ByteCountListener byteListener);

  CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);

  void sendIceMessage(int remotePlayerId, Object message);

  CompletableFuture<List<Replay>> findReplaysByQuery(String condition, int maxResults, int page);

  CompletableFuture<List<MapBean>> findMapsByQuery(String query, int page, int maxSearchResults);

  CompletableFuture<Optional<MapBean>> findMapByFolderName(String folderName);

  CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds);

  CompletableFuture<Void> saveGameReview(Review review, int gameId);

  CompletableFuture<Void> saveModVersionReview(Review review, int modVersionId);

  CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId);

  CompletableFuture<Optional<Replay>> getLastGameOnMap(int playerId, String mapVersionId);

  CompletableFuture<Void> deleteGameReview(Review review);

  CompletableFuture<Optional<Clan>> getClanByTag(String tag);

  Optional<MapBean> findMapById(String id);

  CompletableFuture<Void> deleteMapVersionReview(Review review);

  CompletableFuture<Optional<Replay>> findReplayById(int id);

  void banPlayer(int playerId, int duration, PeriodType periodType, String reason);

  void closePlayersGame(int playerId);

  void closePlayersLobby(int playerId);

  void broadcastMessage(String message);
}
