package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("coopResult")
public class CoopResult {
  @Id
  private String id;
  private Duration duration;
  private String playerNames;
  private boolean secondaryObjectives;
  private int playerCount;
  /** This field is not provided by the API but must be enriched instead. */
  private int ranking;
}
