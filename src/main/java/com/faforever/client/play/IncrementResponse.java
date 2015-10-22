package com.faforever.client.play;

public class IncrementResponse extends UnlockResponse {

  private AchievementState currentState;
  private int currentSteps;

  public AchievementState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(AchievementState currentState) {
    this.currentState = currentState;
  }

  public int getCurrentSteps() {
    return currentSteps;
  }

  public void setCurrentSteps(int currentSteps) {
    this.currentSteps = currentSteps;
  }
}
