package com.faforever.client.play;

import com.google.api.client.util.Key;

import java.util.Date;

public class PlayerAchievement {

  @Key("state")
  private AchievementState state;
  @Key("achievement_id")
  private String achievementId;
  @Key("current_steps")
  private int currentSteps;
  @Key("create_time")
  private long createTime;
  @Key("update_time")
  private long updateTime;

  public AchievementState getState() {
    return state;
  }

  public void setState(AchievementState state) {
    this.state = state;
  }

  public String getAchievementId() {
    return achievementId;
  }

  public void setAchievementId(String achievementId) {
    this.achievementId = achievementId;
  }

  public int getCurrentSteps() {
    return currentSteps;
  }

  public void setCurrentSteps(int currentSteps) {
    this.currentSteps = currentSteps;
  }

  public Date getCreateTime() {
    return new Date(createTime * 1000);
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime.getTime() / 1000;
  }

  public Date getUpdateTime() {
    return new Date(updateTime * 1000);
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime.getTime() / 1000;
  }
}
