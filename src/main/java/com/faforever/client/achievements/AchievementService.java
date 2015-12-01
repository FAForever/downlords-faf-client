package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.PlayerAchievement;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AchievementService {

  ObservableList<PlayerAchievement> getPlayerAchievements(String username);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);

  Image getRevealedIcon(AchievementDefinition achievementDefinition);

  Image getUnlockedIcon(AchievementDefinition achievementDefinition);
}
