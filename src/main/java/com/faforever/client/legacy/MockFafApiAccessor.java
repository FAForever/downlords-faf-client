package com.faforever.client.legacy;

import com.faforever.client.achievements.AchievementDefinition;
import com.faforever.client.achievements.PlayerAchievement;
import com.faforever.client.api.FafApiAccessor;

import java.util.Collections;
import java.util.List;

public class MockFafApiAccessor implements FafApiAccessor {

  @Override
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return Collections.emptyList();
  }

  @Override
  public List<AchievementDefinition> getAchievementDefinitions() {
    return Collections.emptyList();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return null;
  }

  @Override
  public void authorize(int playerId) {

  }
}
