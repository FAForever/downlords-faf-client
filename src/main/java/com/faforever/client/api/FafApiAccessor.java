package com.faforever.client.api;

import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.ModInfoBean;

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

  void authorize(int playerId);

  List<ModInfoBean> getMods();

  MapBean findMapByName(String mapId);

  List<Ranked1v1EntryBean> getRanked1v1Entries();

  Ranked1v1Stats getRanked1v1Stats();

  Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId);

  List<MapBean> getMaps();

  List<MapBean> getMostDownloadedMaps(int count);

  List<MapBean> getMostPlayedMaps(int count);

  List<MapBean> getBestRatedMaps(int count);

  List<MapBean> getNewestMaps(int count);

  void uploadMod(Path file, boolean isRanked, ByteCountListener listener) throws IOException;

  void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException;
}
