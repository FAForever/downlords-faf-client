package com.faforever.client.api.dto;

import com.faforever.client.remote.domain.VictoryCondition;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("game")
public class Game {
  @Id
  private String id;
  private String name;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private Integer replayTicks;
  private Validity validity;
  private VictoryCondition victoryCondition;

  @Relationship("reviews")
  private List<GameReview> reviews;

  @Relationship("playerStats")
  private List<GamePlayerStats> playerStats;

  @Relationship("host")
  private Player host;

  @Relationship("featuredMod")
  private FeaturedMod featuredMod;

  @Relationship("mapVersion")
  private MapVersion mapVersion;

  @Relationship("reviewsSummary")
  private GameReviewsSummary gameReviewsSummary;
}
