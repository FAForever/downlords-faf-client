package com.faforever.client.api;

import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;

import java.io.InputStream;
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

  List<Ranked1v1EntryBean> getRanked1v1Entries();

  Ranked1v1Stats getRanked1v1Stats();

  Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId);

  void uploadMod(InputStream inputStream);
}
