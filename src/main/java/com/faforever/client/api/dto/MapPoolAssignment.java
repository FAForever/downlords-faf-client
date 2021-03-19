package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Type("mapPoolAssignment")
public class MapPoolAssignment extends AbstractEntity {
  @Relationship("mapPool")
  private MapPool mapPool;
  @Relationship("mapVersion")
  private MapVersion mapVersion;
  private int weight;
  private MapParams mapParams;
}


