package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;

import java.util.List;

public class UpdatedAchievementsInfo extends ServerMessage {

  private List<UpdatedAchievement> updatedAchievements;

  public UpdatedAchievementsInfo() {
    super(ServerMessageType.UPDATED_ACHIEVEMENTS);
  }

  public List<UpdatedAchievement> getUpdatedAchievements() {
    return updatedAchievements;
  }

  public void setUpdatedAchievements(List<UpdatedAchievement> updatedAchievements) {
    this.updatedAchievements = updatedAchievements;
  }
}
