package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Type("mapReviewsSummary")
public class MapReviewsSummary extends ReviewsSummary {

  @Relationship("Map")
  private Map map;

}
