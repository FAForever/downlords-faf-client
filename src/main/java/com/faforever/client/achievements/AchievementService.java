package com.faforever.client.achievements;

import javafx.collections.ObservableList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AchievementService {

  ObservableList<PlayerAchievement> getPlayerAchievements(String username);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);
}
