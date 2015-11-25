package com.faforever.client.achievements;

import java.util.Date;

public class PlayerAchievement {

  private String id;
  private AchievementState state;
  private String achievementId;
  private Integer currentSteps;
  private long createTime;
  private long updateTime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setCurrentSteps(Integer currentSteps) {
    this.currentSteps = currentSteps;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

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

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlayerAchievement that = (PlayerAchievement) o;

    return !(id != null ? !id.equals(that.id) : that.id != null);

  }
}
