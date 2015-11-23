package com.faforever.client.api;

import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.mod.ModInfoBean;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  AchievementDefinition getAchievementDefinition(String achievementId);

  List<String> getModNames();

  void authorize(int playerId);

  CompletableFuture<List<ModInfoBean>> getMods();
}
