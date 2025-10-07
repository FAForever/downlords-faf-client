package com.faforever.client.map.generator;

import lombok.Builder;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Builder
public record GeneratorCommand(
    Path javaExecutable,
    Path generatorExecutableFile,
    ComparableVersion version,
    String mapName,
    GeneratorOptions generatorOptions,
    Integer spawnCount,
    Integer numTeams,
    Integer mapSize,
    String seed,
    GenerationType generationType,
    String symmetry,
    String style,
    String terrainStyle,
    String textureStyle,
    String resourceStyle,
    String propStyle, Float reclaimDensity, Float resourceDensity,
    String commandLineArgs
) {

  public List<String> getCommand() {
    String javaPath = javaExecutable.toAbsolutePath().toString();
    if (generatorExecutableFile == null) {
      throw new IllegalStateException("Map generator path not set");
    }
    List<String> command = new ArrayList<>(
        List.of(javaPath, "-jar", generatorExecutableFile.toAbsolutePath().toString()));
    if (version.compareTo(new ComparableVersion("1")) >= 0) {
      if (commandLineArgs != null) {
        command.addAll(Arrays.asList(commandLineArgs.split(" ")));
        return command;
      }

      if (mapName != null) {
        command.addAll(Arrays.asList("--map-name", mapName));
        return command;
      }

      if (mapSize == null || spawnCount == null || numTeams == null) {
        throw new IllegalStateException("Map generation parameters not properly set");
      }

      command.addAll(
          Arrays.asList("--map-size", mapSize.toString(), "--spawn-count", spawnCount.toString(), "--num-teams",
                        numTeams.toString()));

      if (generationType != null && generationType != GenerationType.CASUAL) {
        switch (generationType) {
          case BLIND -> command.add("--blind");
          case TOURNAMENT -> command.add("--tournament-style");
          case UNEXPLORED -> command.add("--unexplored");
        }
        return command;
      }

      if (seed != null) {
        command.addAll(Arrays.asList("--seed", seed));
      }

      if (symmetry != null) {
        command.addAll(Arrays.asList("--terrain-symmetry", symmetry));
      }

      if (style != null) {
        command.addAll(Arrays.asList("--style", style));
        return command;
      }

      if (terrainStyle != null) {
        command.addAll(Arrays.asList("--terrain-style", terrainStyle));
      }

      if (textureStyle != null) {
        command.addAll(Arrays.asList("--texture-style", textureStyle));
      }

      if (resourceStyle != null) {
        command.addAll(Arrays.asList("--resource-style", resourceStyle));
      }

      if (propStyle != null) {
        command.addAll(Arrays.asList("--prop-style", propStyle));
      }

      if (resourceDensity != null) {
        command.addAll(Arrays.asList("--resource-density", String.valueOf(resourceDensity)));
      }

      if (reclaimDensity != null) {
        command.addAll(Arrays.asList("--reclaim-density", String.valueOf(reclaimDensity)));
      }

      return command;
    } else {
      return Arrays.asList(javaPath, "-jar", generatorExecutableFile.toAbsolutePath().toString(), ".",
                           String.valueOf(seed), version.toString(), mapName);
    }
  }
}
