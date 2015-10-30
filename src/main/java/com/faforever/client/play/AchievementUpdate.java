package com.faforever.client.play;

import com.google.api.client.util.Key;

public class AchievementUpdate {

  @Key("update_type")
  private AchievementUpdateType updateType;
  @Key("achievement_id")
  private String achievementId;
  @Key("steps")
  private Integer steps;

  public AchievementUpdate(AchievementUpdateType updateType, String achievementId, Integer steps) {
    this.updateType = updateType;
    this.achievementId = achievementId;
    this.steps = steps;
  }

  public String getAchievementId() {
    return achievementId;
  }

  public void setAchievementId(String achievementId) {
    this.achievementId = achievementId;
  }

  public AchievementUpdateType getUpdateType() {
    return updateType;
  }

  public void setUpdateType(AchievementUpdateType updateType) {
    this.updateType = updateType;
  }

  public Integer getSteps() {
    return steps;
  }

  public void setSteps(Integer steps) {
    this.steps = steps;
  }

  public static AchievementUpdate unlock(String achievementId) {
    return new AchievementUpdate(AchievementUpdateType.UNLOCK, achievementId, null);
  }
}
