package com.faforever.client.achievements;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.PlayerAchievement;
import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AchievementService {

  CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(Integer playerId);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId);

  Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState);

  enum AchievementState {
    HIDDEN, REVEALED, UNLOCKED
  }
}
