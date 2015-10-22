package com.faforever.client.play;

import java.util.Date;

public class PlayerAchievement {

  private AchievementState state;
  private String achievementId;
  private int currentSteps;
  private Date createTime;
  private Date updateTime;

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
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }
}
