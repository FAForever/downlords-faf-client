package com.faforever.client.achievements;

import com.faforever.client.api.AchievementState;
import com.faforever.client.api.PlayerAchievement;

import java.time.LocalDateTime;

public class PlayerAchievementBuilder {

  private final PlayerAchievement playerAchievement;

  private PlayerAchievementBuilder() {
    playerAchievement = new PlayerAchievement();
  }

  public PlayerAchievementBuilder defaultValues() {
    playerAchievement.setAchievementId("1-2-3");
    playerAchievement.setState(AchievementState.REVEALED);
    playerAchievement.setCreateTime(LocalDateTime.now());
    playerAchievement.setUpdateTime(LocalDateTime.now());
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

  public static PlayerAchievementBuilder create() {
    return new PlayerAchievementBuilder();
  }
}
