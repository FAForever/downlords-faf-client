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
@Type("matchmakerQueueMapPool")
public class MatchmakerQueueMapPool {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private int minRating;
  private int maxRating;

  @Relationship("mapPool")
  private MapPool mapPool;
  @Relationship("matchmakerQueue")
  private MatchmakerQueue matchmakerQueue;
}