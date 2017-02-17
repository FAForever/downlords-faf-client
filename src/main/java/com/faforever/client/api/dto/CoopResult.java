package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("coopLeaderboard")
public class CoopResult {
  @Id
  private String id;
  private int duration;
  private String playerNames;
  private boolean secondaryObjectives;
  private int ranking;
  private int playerCount;
}
