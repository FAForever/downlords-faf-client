package com.faforever.client.map.generator;

import lombok.Builder;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Builder
@Value
public class GeneratorCommand {

  Path generatorExecutableFile;
  ComparableVersion version;
  String mapFilename;
  Integer spawnCount;
  Integer numTeams;
  Integer mapSize;
  String seed;
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
  String query;

  public List<String> getCommand() {
    String javaPath = Path.of(System.getProperty("java.home")).resolve("bin").resolve(org.bridj.Platform.isWindows() ? "java.exe" : "java").toAbsolutePath().toString();
    if (generatorExecutableFile == null) {
      throw new IllegalStateException("Map generator path not set");
    }
    List<String> command = new ArrayList<>(List.of(javaPath, "-jar", generatorExecutableFile.toAbsolutePath().toString()));
    if (version.compareTo(new ComparableVersion("1")) >= 0) {
      if (commandLineArgs != null) {
        command.addAll(Arrays.asList(commandLineArgs.split(" ")));
        return command;
      }

      if (query != null) {
        command.add(query);
        return command;
      }

      if (mapFilename == null && (mapSize == null || spawnCount == null || numTeams == null)) {
        throw new IllegalStateException("Map generation parameters not properly set");
      }

      if (mapFilename == null) {
        command.addAll(Arrays.asList("--map-size", mapSize.toString(), "--spawn-count", spawnCount.toString(), "--num-teams", numTeams.toString()));

        if (style != null) {
          command.addAll(Arrays.asList("--style", style));
          return command;
        }

        if (generationType != null) {
          switch (generationType) {
            case BLIND -> command.add("--blind");
            case TOURNAMENT -> command.add("--tournament-style");
            case UNEXPLORED -> command.add("--unexplored");
            default -> {
            }
          }
        }

        if (landDensity != null) {
          command.addAll(Arrays.asList("--land-density", landDensity.toString()));
        }
        if (mountainDensity != null) {
          command.addAll(Arrays.asList("--mountain-density", mountainDensity.toString()));
        }
        if (plateauDensity != null) {
          command.addAll(Arrays.asList("--plateau-density", plateauDensity.toString()));
        }
        if (rampDensity != null) {
          command.addAll(Arrays.asList("--ramp-density", rampDensity.toString()));
        }
        if (mexDensity != null) {
          command.addAll(Arrays.asList("--mex-density", mexDensity.toString()));
        }
        if (reclaimDensity != null) {
          command.addAll(Arrays.asList("--reclaim-density", reclaimDensity.toString()));
        }
        if (biome != null) {
          command.addAll(Arrays.asList("--biome", biome));
        }

      } else {
        command.addAll(Arrays.asList("--map-name", mapFilename));
      }

      return command;
    } else {
      return Arrays.asList(javaPath, "-jar", generatorExecutableFile.toAbsolutePath().toString(), ".",
          String.valueOf(seed), version.toString(), mapFilename);
    }
  }
}
