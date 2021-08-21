package com.faforever.client.builders;

import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.PlayerAchievement;

import java.time.OffsetDateTime;

public class PlayerAchievementBuilder {

  private final PlayerAchievement playerAchievement;

  private PlayerAchievementBuilder() {
    playerAchievement = new PlayerAchievement();
  }

  public static PlayerAchievementBuilder create() {
    return new PlayerAchievementBuilder();
  }

  public PlayerAchievementBuilder defaultValues() {
    playerAchievement.setAchievement(new AchievementDefinition().setId("1-2-3"));
    playerAchievement.setState(AchievementState.REVEALED);
    playerAchievement.setCreateTime(OffsetDateTime.now());
    playerAchievement.setUpdateTime(OffsetDateTime.now());
    return this;
  }

  public PlayerAchievementBuilder state(AchievementState state) {
    playerAchievement.setState(state);
    return this;
  }

  public PlayerAchievementBuilder currentSteps(int steps) {
    playerAchievement.setCurrentSteps(steps);
    return this;
  }

  public PlayerAchievementBuilder achievementId(String achievementId) {
    playerAchievement.setAchievement(new AchievementDefinition().setId(achievementId));
    return this;
  }

  public PlayerAchievement get() {
    return playerAchievement;
  }
}
