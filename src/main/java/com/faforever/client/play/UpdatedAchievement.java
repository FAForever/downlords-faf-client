package com.faforever.client.play;

import com.google.api.client.util.Key;
import org.jetbrains.annotations.Nullable;

public class UpdatedAchievement {

  @Key("achievement_id")
  private String achievementId;
  @Key("current_state")
  private AchievementState currentState;
  @Key("current_steps")
  private Integer currentSteps;
  @Key("newly_unlocked")
  private boolean newlyUnlocked;

  public String getAchievementId() {
    return achievementId;
  }

  public void setAchievementId(String achievementId) {
    this.achievementId = achievementId;
  }

  public AchievementState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(AchievementState currentState) {
    this.currentState = currentState;
  }

  @Nullable
  public Integer getCurrentSteps() {
    return currentSteps;
  }

  public void setCurrentSteps(@Nullable Integer currentSteps) {
    this.currentSteps = currentSteps;
  }

  public boolean getNewlyUnlocked() {
    return newlyUnlocked;
  }

  public void setNewlyUnlocked(boolean newlyUnlocked) {
    this.newlyUnlocked = newlyUnlocked;
  }
}
