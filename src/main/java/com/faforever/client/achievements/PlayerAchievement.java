package com.faforever.client.achievements;

import java.util.Date;

public class PlayerAchievement {

  private AchievementState state;
  private String achievementId;
  private Integer currentSteps;
  private long createTime;
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

  public Integer getCurrentSteps() {
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
