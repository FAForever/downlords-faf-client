package com.faforever.client.map.generator;

import lombok.Builder;

@Builder
public record GeneratorOptions(
    Integer spawnCount,
    Integer numTeams,
    Integer mapSize,
    String seed,
    GenerationType generationType,
    String symmetry,
    String style,
    String terrainStyle,
    String textureStyle,
    String resourceStyle, String propStyle, Float reclaimDensity, Float resourceDensity,
    String commandLineArgs,
    Integer numToGenerate
) {}
