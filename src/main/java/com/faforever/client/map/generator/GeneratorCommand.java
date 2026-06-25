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
    String propStyle, Float reclaimDensity, Float resourceDensity, String commandLineArgs, Integer numToGenerate
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
        if (numToGenerate != null && numToGenerate > 1) {
          command.addAll(Arrays.asList("--num-to-generate", String.valueOf(numToGenerate)));
        }
        return command;
      }

      if (mapName != null) {
        command.addAll(Arrays.asList("--map-name", mapName));
        return command;
      }

      // Use values from generatorOptions if available and individual fields are not set
      Integer effectiveSpawnCount = spawnCount != null ? spawnCount : (generatorOptions != null ? generatorOptions.spawnCount() : null);
      Integer effectiveNumTeams = numTeams != null ? numTeams : (generatorOptions != null ? generatorOptions.numTeams() : null);
      Integer effectiveMapSize = mapSize != null ? mapSize : (generatorOptions != null ? generatorOptions.mapSize() : null);

      if (effectiveMapSize == null || effectiveSpawnCount == null || effectiveNumTeams == null) {
        throw new IllegalStateException("Map generation parameters not properly set");
      }

      command.addAll(
          Arrays.asList("--map-size", effectiveMapSize.toString(), "--spawn-count", effectiveSpawnCount.toString(),
                        "--num-teams", effectiveNumTeams.toString()));

      if (generationType != null && generationType != GenerationType.CASUAL) {
        switch (generationType) {
          case BLIND -> command.add("--blind");
          case TOURNAMENT -> command.add("--tournament-style");
          case UNEXPLORED -> command.add("--unexplored");
        }
        return command;
      }

      if (numToGenerate != null && numToGenerate > 1) {
        command.addAll(Arrays.asList("--num-to-generate", String.valueOf(numToGenerate)));
      } else if (seed != null) {
        command.addAll(Arrays.asList("--seed", seed));
      } else if (generatorOptions != null && generatorOptions.seed() != null) {
        command.addAll(Arrays.asList("--seed", generatorOptions.seed()));
      }

      if (symmetry != null) {
        command.addAll(Arrays.asList("--terrain-symmetry", symmetry));
      } else if (generatorOptions != null && generatorOptions.symmetry() != null) {
        command.addAll(Arrays.asList("--terrain-symmetry", generatorOptions.symmetry()));
      }

      if (style != null) {
        command.addAll(Arrays.asList("--style", style));
        return command;
      } else if (generatorOptions != null && generatorOptions.style() != null) {
        command.addAll(Arrays.asList("--style", generatorOptions.style()));
        return command;
      }

      if (terrainStyle != null) {
        command.addAll(Arrays.asList("--terrain-style", terrainStyle));
      } else if (generatorOptions != null && generatorOptions.terrainStyle() != null) {
        command.addAll(Arrays.asList("--terrain-style", generatorOptions.terrainStyle()));
      }

      if (textureStyle != null) {
        command.addAll(Arrays.asList("--texture-style", textureStyle));
      } else if (generatorOptions != null && generatorOptions.textureStyle() != null) {
        command.addAll(Arrays.asList("--texture-style", generatorOptions.textureStyle()));
      }

      if (resourceStyle != null) {
        command.addAll(Arrays.asList("--resource-style", resourceStyle));
      } else if (generatorOptions != null && generatorOptions.resourceStyle() != null) {
        command.addAll(Arrays.asList("--resource-style", generatorOptions.resourceStyle()));
      }

      if (propStyle != null) {
        command.addAll(Arrays.asList("--prop-style", propStyle));
      } else if (generatorOptions != null && generatorOptions.propStyle() != null) {
        command.addAll(Arrays.asList("--prop-style", generatorOptions.propStyle()));
      }

      if (resourceDensity != null) {
        command.addAll(Arrays.asList("--resource-density", String.valueOf(resourceDensity)));
      } else if (generatorOptions != null && generatorOptions.resourceDensity() != null) {
        command.addAll(Arrays.asList("--resource-density", String.valueOf(generatorOptions.resourceDensity())));
      }

      if (reclaimDensity != null) {
        command.addAll(Arrays.asList("--reclaim-density", String.valueOf(reclaimDensity)));
      } else if (generatorOptions != null && generatorOptions.reclaimDensity() != null) {
        command.addAll(Arrays.asList("--reclaim-density", String.valueOf(generatorOptions.reclaimDensity())));
      }

      if (commandLineArgs != null) {
        command.addAll(Arrays.asList(commandLineArgs.split(" ")));
      } else if (generatorOptions != null && generatorOptions.commandLineArgs() != null) {
        command.addAll(Arrays.asList(generatorOptions.commandLineArgs().split(" ")));
      }

      return command;
    } else {
      return Arrays.asList(javaPath, "-jar", generatorExecutableFile.toAbsolutePath().toString(), ".", String.valueOf(
                               seed != null ? seed : (generatorOptions != null ? generatorOptions.seed() : "")), version.toString(),
                           mapName);
    }
  }
}
