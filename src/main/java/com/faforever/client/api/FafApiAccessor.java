package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.Clan;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1Map;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.MeResult;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModVersion;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.api.dto.Tournament;
import com.faforever.client.api.dto.TutorialCategory;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
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

  @SuppressWarnings("unchecked")
  List<PlayerEvent> getPlayerEvents(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  AchievementDefinition getAchievementDefinition(String achievementId);

  void authorize(int playerId, String username, String password);

  List<Mod> getMods();

  List<com.faforever.client.api.dto.FeaturedMod> getFeaturedMods();

  List<Ladder1v1LeaderboardEntry> getLadder1v1Leaderboard();

  List<GlobalLeaderboardEntry> getGlobalLeaderboard();

  Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId);

  List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod);

  Tuple<List<Map>, java.util.Map<String, ?>> getMapsByIdWithMeta(List<Integer> mapIdList, int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getMostPlayedMapsWithMeta(int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getHighestRatedMapsWithMeta(int count, int page);

  Tuple<List<Map>, java.util.Map<String, ?>> getNewestMapsWithMeta(int count, int page);

  List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count);

  void uploadMod(Path file, ByteCountListener listener);

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  List<CoopMission> getCoopMissions();

  List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers);

  void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException;

  ModVersion getModVersion(String uid);

  List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  Tuple<List<Game>, java.util.Map<String, ?>> getNewestReplaysWithMeta(int count, int page);

  Tuple<List<Game>, java.util.Map<String, ?>> getHighestRatedReplaysWithMeta(int count, int page);

  Tuple<List<Game>, java.util.Map<String, ?>> findReplaysByQueryWithMeta(String query, int maxResults, int page, SortConfig sortConfig);

  Optional<MapVersion> findMapByFolderName(String folderName);

  List<com.faforever.client.api.dto.Player> getPlayersByIds(Collection<Integer> playerIds);

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

  Tuple<List<Ladder1v1Map>, java.util.Map<String, ?>> getLadder1v1MapsWithMeta(int count, int page);

  List<Tournament> getAllTournaments();

  Tuple<List<MapVersion>, java.util.Map<String, ?>> getOwnedMapsWithMeta(int playerId, int loadMoreCount, int page);

  void updateMapVersion(String id, MapVersion mapVersion);

  MeResult getOwnPlayer();
}
