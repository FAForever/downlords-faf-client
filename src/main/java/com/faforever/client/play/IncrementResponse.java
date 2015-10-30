package com.faforever.client.play;

import com.google.api.client.util.Key;

public class IncrementResponse extends UnlockResponse {

  @Key("current_state")
  private AchievementState currentState;
  @Key("current_steps")
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
