package com.faforever.client.api;

import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.util.Tuple;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Provides access to the FAF REST API. Services should not access this class directly, but use {@link
 * com.faforever.client.remote.FafService} instead.
 */
public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<PlayerEvent> getPlayerEvents(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  AchievementDefinition getAchievementDefinition(String achievementId);

  void authorize();

  List<Mod> getMods();

  List<com.faforever.commons.api.dto.FeaturedMod> getFeaturedMods();

  List<Leaderboard> getLeaderboards();

  List<LeaderboardEntry> getAllLeaderboardEntries(String leaderboardTechnicalName);

  Tuple<List<LeaderboardEntry>, java.util.Map<String, ?>> getLeaderboardEntriesWithMeta(String leaderboardTechnicalName, int count, int page);

  List<LeaderboardEntry> getLeaderboardEntriesForPlayer(int playerId);

  List<LeaderboardRatingJournal> getRatingJournal(int playerId, int leaderboardId);

  Tuple<List<Map>, java.util.Map<String, ?>> getMapsByIdWithMeta(List<Integer> mapIdList, int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getRecommendedMapsWithMeta(int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getMostPlayedMapsWithMeta(int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getHighestRatedMapsWithMeta(int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getNewestMapsWithMeta(int count, int page);

  List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count);

  void uploadMod(Path file, ByteCountListener listener);

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  List<CoopMission> getCoopMissions();

  List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers);

  void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException;

  Optional<ModVersion> getModVersion(String uid);

  List<com.faforever.commons.api.dto.FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  Tuple<List<Game>, java.util.Map<String, ?>> getNewestReplaysWithMeta(int count, int page);

  Tuple<List<Game>, java.util.Map<String, ?>> getHighestRatedReplaysWithMeta(int count, int page);

  Tuple<List<Game>, java.util.Map<String, ?>> findReplaysByQueryWithMeta(String query, int maxResults, int page, SortConfig sortConfig);

  Optional<MapVersion> findMapByFolderName(String folderName);

  Optional<MapVersion> getMapLatestVersion(String mapFolderName);

  List<Player> getPlayersByIds(Collection<Integer> playerIds);

  Optional<Player> queryPlayerByName(String playerName);

  GameReview createGameReview(GameReview review);

  void updateGameReview(GameReview review);

  ModVersionReview createModVersionReview(ModVersionReview review);

  void updateModVersionReview(ModVersionReview review);

  MapVersionReview createMapVersionReview(MapVersionReview review);

  void updateMapVersionReview(MapVersionReview review);

  void deleteGameReview(String id);

  List<TutorialCategory> getTutorialCategories();

  Optional<Clan> getClanByTag(String tag);

  Tuple<List<Map>, java.util.Map<String, ?>> findMapsByQueryWithMeta(SearchConfig searchConfig, int count, int page);

  Optional<MapVersion> findMapVersionById(String id);

  void deleteMapVersionReview(String id);

  void deleteModVersionReview(String id);

  Optional<Game> findReplayById(int id);

  Tuple<List<Mod>, java.util.Map<String, ?>> findModsByQueryWithMeta(SearchConfig query, int maxResults, int page);

  Tuple<List<Mod>, java.util.Map<String, ?>> getRecommendedModsWithMeta(int count, int page);

  List<MapPoolAssignment> getMatchmakerPoolMaps(int matchmakerQueueId, float rating);

  Optional<MatchmakerQueue> getMatchmakerQueue(String technicalName);

  List<Tournament> getAllTournaments();

  List<ModerationReport> getPlayerModerationReports(int playerId);

  void postModerationReport(com.faforever.client.reporting.ModerationReport report);

  Tuple<List<MapVersion>, java.util.Map<String, ?>> getOwnedMapsWithMeta(int playerId, int loadMoreCount, int page);

  void updateMapVersion(String id, MapVersion mapVersion);

  MeResult verifyUser();
}
