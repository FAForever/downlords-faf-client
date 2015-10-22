package com.faforever.client.play;

public class AchievementDefinition {

  private String description;
  private int experiencePoints;
  private String id;
  private AchievementState initialState;
  private String name;
  private String revealedIconUrl;
  private Integer totalSteps;
  private AchievementType type;
  private String unlockedIconUrl;

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
}
