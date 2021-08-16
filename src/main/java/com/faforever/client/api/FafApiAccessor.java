package com.faforever.client.api;

import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.Clan;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.dto.PlayerEvent;
import com.faforever.commons.api.dto.Tournament;
import com.faforever.commons.api.dto.TutorialCategory;
import com.faforever.commons.io.ByteCountListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Provides access to the FAF REST API. Services should not access this class directly, but use {@link
 * com.faforever.client.remote.FafService} instead.
 */
public interface FafApiAccessor {

  void authorize();

  Flux<PlayerAchievement> getPlayerAchievements(int playerId);

  Flux<PlayerEvent> getPlayerEvents(int playerId);

  Flux<AchievementDefinition> getAchievementDefinitions();

  Mono<AchievementDefinition> getAchievementDefinition(String achievementId);

  Flux<Mod> getMods();

  Flux<com.faforever.commons.api.dto.FeaturedMod> getFeaturedMods();

  Flux<Leaderboard> getLeaderboards();

  Flux<LeaderboardEntry> getAllLeaderboardEntries(String leaderboardTechnicalName);

  Mono<Tuple2<List<LeaderboardEntry>, Integer>> getLeaderboardEntriesWithTotalPages(String leaderboardTechnicalName, int count, int page);

  Flux<LeaderboardEntry> getLeaderboardEntriesForPlayer(int playerId);

  Flux<LeaderboardRatingJournal> getRatingJournal(int playerId, int leaderboardId);

  Mono<Tuple2<List<Map>, Integer>> getMapsByIdWithTotalPages(List<Integer> mapIdList, int count, int page);

  Mono<Tuple2<List<Map>, Integer>> getRecommendedMapsWithTotalPages(int count, int page);

  Mono<Tuple2<List<Map>, Integer>> getMostPlayedMapsWithTotalPages(int count, int page);

  Mono<Tuple2<List<Map>, Integer>> getHighestRatedMapsWithTotalPages(int count, int page);

  Mono<Tuple2<List<Map>, Integer>> getNewestMapsWithTotalPages(int count, int page);

  Flux<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count);

  Mono<Void> uploadMod(Path file, ByteCountListener listener);

  Mono<Void> uploadMap(Path file, boolean isRanked, ByteCountListener listener);

  Flux<CoopMission> getCoopMissions();

  Flux<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers);

  Mono<ModVersion> getModVersion(String uid);

  Flux<com.faforever.commons.api.dto.FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  Mono<Tuple2<List<Game>, Integer>> getNewestReplaysWithTotalPages(int count, int page);

  Mono<Tuple2<List<Game>, Integer>> getHighestRatedReplaysWithTotalPages(int count, int page);

  Mono<Tuple2<List<Game>, Integer>> findReplaysByQueryWithTotalPages(String query, int maxResults, int page, SortConfig sortConfig);

  Mono<MapVersion> findMapByFolderName(String folderName);

  Mono<MapVersion> getMapLatestVersion(String mapFolderName);

  Flux<Player> getPlayersByIds(Collection<Integer> playerIds);

  Mono<Player> queryPlayerByName(String playerName);

  Mono<GameReview> createGameReview(GameReview review);

  Mono<Void> updateGameReview(GameReview review);

  Mono<ModVersionReview> createModVersionReview(ModVersionReview review);

  Mono<Void> updateModVersionReview(ModVersionReview review);

  Mono<MapVersionReview> createMapVersionReview(MapVersionReview review);

  Mono<Void> updateMapVersionReview(MapVersionReview review);

  Mono<Void> deleteGameReview(String id);

  Flux<TutorialCategory> getTutorialCategories();

  Mono<Clan> getClanByTag(String tag);

  Mono<Tuple2<List<Map>, Integer>> findMapsByQueryWithTotalPages(SearchConfig searchConfig, int count, int page);

  Mono<Void> deleteMapVersionReview(String id);

  Mono<Void> deleteModVersionReview(String id);

  Mono<Game> findReplayById(int id);

  Mono<Tuple2<List<Mod>, Integer>> findModsByQueryWithTotalPages(SearchConfig query, int maxResults, int page);

  Mono<Tuple2<List<Mod>, Integer>> getRecommendedModsWithTotalPages(int count, int page);

  Flux<MapPoolAssignment> getMatchmakerPoolMaps(int matchmakerQueueId, float rating);

  Mono<MatchmakerQueue> getMatchmakerQueue(String technicalName);

  Flux<Tournament> getAllTournaments();

  Flux<ModerationReport> getPlayerModerationReports(int playerId);

  Mono<Void> postModerationReport(com.faforever.client.reporting.ModerationReport report);

  Mono<Tuple2<List<MapVersion>, Integer>> getOwnedMapsWithTotalPages(int playerId, int loadMoreCount, int page);

  Mono<Void> updateMapVersion(String id, MapVersion mapVersion);

  Mono<MeResult> getMe();
}
