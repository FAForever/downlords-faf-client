package com.faforever.client.api.dto;


import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.Instant;

@Data
@Type("playerAchievement")
public class PlayerAchievement {

  @Id
  private String id;
  private AchievementState state;
  private Integer currentSteps;
  private Instant createTime;
  private Instant updateTime;

  @Relationship("achievement")
  private AchievementDefinition achievement;
}
