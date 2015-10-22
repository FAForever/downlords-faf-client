package com.faforever.client.play;

import java.util.List;

public class AchievementsUpdateResponse {

  private List<UpdatedAchievement> updatedAchievements;

  public List<UpdatedAchievement> getUpdatedAchievements() {
    return updatedAchievements;
  }

  public void setUpdatedAchievements(List<UpdatedAchievement> updatedAchievements) {
    this.updatedAchievements = updatedAchievements;
  }
}
