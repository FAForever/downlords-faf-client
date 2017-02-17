package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("mapStatistics")
public class MapStatistics {
  @Id
  private String id;
  private int downloads;
  private int draws;
  private int plays;

  @Relationship("map")
  private Map map;
}
