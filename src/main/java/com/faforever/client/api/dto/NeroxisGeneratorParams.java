package com.faforever.client.api.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@JsonTypeName("neroxis")
public class NeroxisGeneratorParams extends MapParams {
  private int spawns;
  private int size;
  private String version;
}
