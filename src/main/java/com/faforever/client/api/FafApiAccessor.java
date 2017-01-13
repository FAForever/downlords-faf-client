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
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
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

  List<Map> getAllMaps();

  List<Map> getMostDownloadedMaps(int count);

  List<Map> getMostPlayedMaps(int count);

  List<Map> getHighestRatedMaps(int count);

  List<Map> getNewestMaps(int count);

  List<Game> getLastGamesOnMap(int playerId, int mapVersionId, int count);

  void uploadMod(Path file, ByteCountListener listener);

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  List<CoopMission> getCoopMissions();

  List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers);

  void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException;

  Mod getMod(String uid);

  List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version);

  List<Game> getNewestReplays(int count);

  List<Game> getHighestRatedReplays(int count);

  List<Game> getMostWatchedReplays(int count);

  List<Game> findReplaysByQuery(String condition, int maxResults);

  Optional<MapBean> findMapByFolderName(String folderName);

  List<com.faforever.client.api.dto.Player> getPlayersByIds(Collection<Integer> playerIds);

  GameReview createGameReview(GameReview review);

  void updateGameReview(GameReview review);

  ModVersionReview createModVersionReview(ModVersionReview review);

  void updateModVersionReview(ModVersionReview review);

  MapVersionReview createMapVersionReview(MapVersionReview review);

  void updateMapVersionReview(MapVersionReview review);

  void deleteGameReview(int id);

  Optional<Clan> getClanByTag(String tag);
}
