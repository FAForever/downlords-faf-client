package com.faforever.client.api;

import com.faforever.client.mod.ModInfoBean;

import java.util.List;

public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  AchievementDefinition getAchievementDefinition(String achievementId);

  void authorize(int playerId);

  List<ModInfoBean> getMods();
}
