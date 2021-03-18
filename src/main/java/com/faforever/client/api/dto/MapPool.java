package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Type("mapPool")
public class MapPool {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String name;
  @Relationship("mapVersions")
  @Deprecated
  private List<MapVersion> mapVersions;
  @Relationship("matchmakerQueueMapPool")
  @JsonIgnore
  private MatchmakerQueueMapPool matchmakerQueueMapPool;
  @Relationship("mapPoolAssignments")
  private List<MapPoolAssignment> mapPoolAssignments;
}