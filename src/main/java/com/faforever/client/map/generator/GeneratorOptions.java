package com.faforever.client.map.generator;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GeneratorOptions {
  Integer spawnCount;
  Integer numTeams;
  Integer mapSize;
  Float landDensity;
  Float plateauDensity;
  Float mountainDensity;
  Float rampDensity;
  Float mexDensity;
  Float reclaimDensity;
  GenerationType generationType;
  String style;
  String biome;
  String commandLineArgs;
}
