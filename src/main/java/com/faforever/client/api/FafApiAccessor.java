package com.faforever.client.api;

import com.faforever.client.achievements.AchievementDefinition;
import com.faforever.client.achievements.PlayerAchievement;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  AchievementDefinition getAchievementDefinition(String achievementId);

  List<String> getModNames();

  void authorize(int playerId);

  Collection<Mod> getMods();
}
