package com.faforever.client.api;

import com.google.api.client.util.Key;

public class AchievementDefinition {

  @Key("description")
  private String description;
  @Key("experience_points")
  private int experiencePoints;
  @Key("id")
  private String id;
  @Key("initial_state")
  private AchievementState initialState;
  @Key("name")
  private String name;
  @Key("revealed_icon_url")
  private String revealedIconUrl;
  @Key("total_steps")
  private Integer totalSteps;
  @Key("type")
  private AchievementType type;
  @Key("unlocked_icon_url")
  private String unlockedIconUrl;
  @Key
  private int order;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getExperiencePoints() {
    return experiencePoints;
  }

  public void setExperiencePoints(int experiencePoints) {
    this.experiencePoints = experiencePoints;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public AchievementState getInitialState() {
    return initialState;
  }

  public void setInitialState(AchievementState initialState) {
    this.initialState = initialState;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRevealedIconUrl() {
    return revealedIconUrl;
  }

  public void setRevealedIconUrl(String revealedIconUrl) {
    this.revealedIconUrl = revealedIconUrl;
  }

  public Integer getTotalSteps() {
    return totalSteps;
  }

  public void setTotalSteps(Integer totalSteps) {
    this.totalSteps = totalSteps;
  }

  public AchievementType getType() {
    return type;
  }

  public void setType(AchievementType type) {
    this.type = type;
  }

  public String getUnlockedIconUrl() {
    return unlockedIconUrl;
  }

  public void setUnlockedIconUrl(String unlockedIconUrl) {
    this.unlockedIconUrl = unlockedIconUrl;
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

    AchievementDefinition that = (AchievementDefinition) o;

    return !(id != null ? !id.equals(that.id) : that.id != null);

  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }
}
