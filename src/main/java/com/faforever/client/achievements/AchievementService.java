package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.PlayerAchievement;
import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface AchievementService {

  CompletionStage<List<PlayerAchievement>> getPlayerAchievements(String username);

  CompletionStage<List<AchievementDefinition>> getAchievementDefinitions();

  CompletionStage<AchievementDefinition> getAchievementDefinition(String achievementId);

  Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState);

  enum AchievementState {
    HIDDEN, REVEALED, UNLOCKED
  }
}
