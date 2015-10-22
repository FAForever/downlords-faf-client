package com.faforever.client.play;

public class UpdatedAchievement {

  private String achievementId;
  private AchievementState currentState;
  private Integer currentSteps;
  private Boolean newlyUnlocked;

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

  public Integer getCurrentSteps() {
    return currentSteps;
  }

  public void setCurrentSteps(Integer currentSteps) {
    this.currentSteps = currentSteps;
  }

  public Boolean getNewlyUnlocked() {
    return newlyUnlocked;
  }

  public void setNewlyUnlocked(Boolean newlyUnlocked) {
    this.newlyUnlocked = newlyUnlocked;
  }
}
