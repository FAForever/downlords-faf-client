package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;

import java.util.List;

public class UpdatedAchievementsMessageLobby extends FafServerMessage {

  private List<UpdatedAchievement> updatedAchievements;

  public UpdatedAchievementsMessageLobby() {
    super(FafServerMessageType.UPDATED_ACHIEVEMENTS);
  }

  public List<UpdatedAchievement> getUpdatedAchievements() {
    return updatedAchievements;
  }

  public void setUpdatedAchievements(List<UpdatedAchievement> updatedAchievements) {
    this.updatedAchievements = updatedAchievements;
  }
}
