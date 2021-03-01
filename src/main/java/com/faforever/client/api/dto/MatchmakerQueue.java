package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Type("matchmakerQueue")
public class MatchmakerQueue {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String nameKey;
  private String technicalName;

  @Relationship("featuredMod")
  private FeaturedMod featuredMod;
  @Relationship("leaderboard")
  private Leaderboard leaderboard;
}