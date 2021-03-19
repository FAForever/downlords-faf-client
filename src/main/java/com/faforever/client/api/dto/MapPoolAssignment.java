package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  @JsonGetter("mapParams")
  public String getParams() throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(mapParams);
  }

  @JsonSetter("mapParams")
  public void setParams(String mapParams) throws JsonProcessingException {
    if (mapParams != null) {
      this.mapParams = new ObjectMapper().readValue(mapParams, MapParams.class);
    } else {
      this.mapParams = null;
    }
  }
}


