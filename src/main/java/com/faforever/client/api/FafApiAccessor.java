package com.faforever.client.api;

import com.faforever.client.coop.CoopMission;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.Mod;
import com.faforever.client.replay.Replay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

  List<FeaturedMod> getFeaturedMods();

  MapBean findMapByName(String mapId);

  List<Ranked1v1EntryBean> getLeaderboardEntries(RatingType ratingType);

  Ranked1v1Stats getRanked1v1Stats();

  Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId);

  History getRatingHistory(RatingType ratingType, int playerId);

  List<MapBean> getMaps();

  List<MapBean> getMostDownloadedMaps(int count);

  List<MapBean> getMostPlayedMaps(int count);

  List<MapBean> getBestRatedMaps(int count);

  List<MapBean> getNewestMaps(int count);

  void uploadMod(Path file, ByteCountListener listener);

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  List<CoopMission> getCoopMissions();

  List<CoopLeaderboardEntry> getCoopLeaderboard(String missionId, int numberOfPlayers);

  void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException;

  Mod getMod(String uid);

  List<FeaturedModFile> getFeaturedModFiles(FeaturedModBean featuredModBean, Integer version);

  List<Replay> searchReplayByPlayer(String playerName);

  List<Replay> searchReplayByMap(String mapName);

  List<Replay> searchReplayByMod(FeaturedMod featuredMod);

  List<Replay> getNewestReplays(int count);

  List<Replay> getHighestRatedReplays(int count);

  List<Replay> getMostWatchedReplays(int count);
}
