package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("achievementDefinition")
public class AchievementDefinition {

  @Id
  private String id;
  private String description;
  private int experiencePoints;
  private AchievementState initialState;
  private String name;
  private String revealedIconUrl;
  private Integer totalSteps;
  private AchievementType type;
  private String unlockedIconUrl;
  private int order;
}
