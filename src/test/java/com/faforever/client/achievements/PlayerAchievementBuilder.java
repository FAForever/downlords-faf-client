package com.faforever.client.achievements;

import com.faforever.client.api.dto.AchievementState;
import com.faforever.client.api.dto.PlayerAchievement;

import java.time.Instant;

public class PlayerAchievementBuilder {

  private final PlayerAchievement playerAchievement;

  private PlayerAchievementBuilder() {
    playerAchievement = new PlayerAchievement();
  }

  public static PlayerAchievementBuilder create() {
    return new PlayerAchievementBuilder();
  }

  public PlayerAchievementBuilder defaultValues() {
    playerAchievement.setAchievementId("1-2-3");
    playerAchievement.setState(AchievementState.REVEALED);
    playerAchievement.setCreateTime(Instant.now());
    playerAchievement.setUpdateTime(Instant.now());
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
    playerAchievement.setAchievementId(achievementId);
    return this;
  }

  public PlayerAchievement get() {
    return playerAchievement;
  }
}
