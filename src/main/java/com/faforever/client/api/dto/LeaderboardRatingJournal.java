package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("leaderboardRatingJournal")
public class LeaderboardRatingJournal {
  @Id
  private String id;
  private Double meanAfter;
  private Double deviationAfter;
  private Double meanBefore;
  private Double deviationBefore;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;

  @Relationship("gamePlayerStats")
  private GamePlayerStats gamePlayerStats;

  @Relationship("leaderboard")
  private Leaderboard leaderboard;
}
