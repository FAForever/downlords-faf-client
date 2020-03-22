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

  String authorize(String username, String password);

  void authorize(String refreshToken);

  List<Mod> getMods();

  List<com.faforever.client.api.dto.FeaturedMod> getFeaturedMods();

  List<Ladder1v1LeaderboardEntry> getLadder1v1Leaderboard();

  List<GlobalLeaderboardEntry> getGlobalLeaderboard();

  Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId);

  List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod);

  List<Map> getMapsById(List<Integer> mapIdList, int count, int page);

  List<Map> getMostPlayedMaps(int count, int page);

  List<Map> getHighestRatedMaps(int count, int page);

  List<Map> getNewestMaps(int count, int page);

  List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count);

  void uploadMod(Path file, ByteCountListener listener);

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  List<CoopMission> getCoopMissions();

  List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers);

  void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException;

  ModVersion getModVersion(String uid);

  List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  List<Game> getNewestReplays(int count, int page);

  List<Game> getHighestRatedReplays(int count, int page);

  List<Game> findReplaysByQuery(String condition, int maxResults, int page, SortConfig sortConfig);

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

  List<Map> findMapsByQuery(SearchConfig searchConfig, int page, int count);

  Optional<MapVersion> findMapVersionById(String id);

  void deleteMapVersionReview(String id);

  void deleteModVersionReview(String id);

  Optional<Game> findReplayById(int id);

  List<Mod> findModsByQuery(SearchConfig query, int page, int maxResults);

  List<Ladder1v1Map> getLadder1v1Maps(int count, int page);

  List<Tournament> getAllTournaments();

  List<MapVersion> getOwnedMaps(int playerId, int loadMoreCount, int page);

  void updateMapVersion(String id, MapVersion mapVersion);

  MeResult getOwnPlayer();
}
