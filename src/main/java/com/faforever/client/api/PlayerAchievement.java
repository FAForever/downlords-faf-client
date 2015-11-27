package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerAchievement {

  @Key
  private String id;
  @Key
  private AchievementState state;
  @Key("achievement_id")
  private String achievementId;
  @Key("current_steps")
  private Integer currentSteps;
  @Key("create_time")
  private String createTime;
  @Key("update_time")
  private String updateTime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setCurrentSteps(Integer currentSteps) {
    this.currentSteps = currentSteps;
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

  public LocalDateTime getCreateTime() {
    return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(createTime));
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = DateTimeFormatter.ISO_DATE_TIME.format(createTime);
  }

  public LocalDateTime getUpdateTime() {
    return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(updateTime));
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = DateTimeFormatter.ISO_DATE_TIME.format(updateTime);
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
