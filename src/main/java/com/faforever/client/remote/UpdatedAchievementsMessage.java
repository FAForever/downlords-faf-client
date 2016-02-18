package com.faforever.client.remote;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;

import java.util.List;

public class UpdatedAchievementsMessage extends FafServerMessage {

  private List<UpdatedAchievement> updatedAchievements;

  public UpdatedAchievementsMessage() {
    super(FafServerMessageType.UPDATED_ACHIEVEMENTS);
  }

  public List<UpdatedAchievement> getUpdatedAchievements() {
    return updatedAchievements;
  }

  public void setUpdatedAchievements(List<UpdatedAchievement> updatedAchievements) {
    this.updatedAchievements = updatedAchievements;
  }
}
