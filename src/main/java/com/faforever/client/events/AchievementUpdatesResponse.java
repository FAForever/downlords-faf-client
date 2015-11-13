package com.faforever.client.events;

import com.google.api.client.util.Key;

import java.util.List;

public class AchievementUpdatesResponse {

  @Key("updated_achievements")
  private List<UpdatedAchievement> updatedAchievements;

  public List<UpdatedAchievement> getUpdatedAchievements() {
    return updatedAchievements;
  }

  public void setUpdatedAchievements(List<UpdatedAchievement> updatedAchievements) {
    this.updatedAchievements = updatedAchievements;
  }
}
