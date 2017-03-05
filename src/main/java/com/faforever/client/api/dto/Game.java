package com.faforever.client.api.dto;

import com.faforever.client.remote.domain.VictoryCondition;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("game")
public class Game {
  @Id
  private int id;
  private String name;
  private Instant startTime;
  private Rankiness rankiness;
  private VictoryCondition victoryCondition;
  private Player host;
  private FeaturedMod featuredMod;
  private MapVersion mapVersion;
  private List<GamePlayerStats> playerStats;
}
