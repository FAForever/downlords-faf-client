package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeName("neroxis")
public class NeroxisGeneratorParams implements MapParams {
  private Integer spawns;
  private Integer size;
  private String version;
}
