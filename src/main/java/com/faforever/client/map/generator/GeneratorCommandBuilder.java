package com.faforever.client.map.generator;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneratorCommandBuilder {

  private Path generatorExecutableFile;
  private ComparableVersion version;
  private String mapFilename;
  private Integer spawnCount;
  private Integer mapSize;
  private String seed;
  private Float landDensity;
  private Float plateauDensity;
  private Float mountainDensity;
  private Float rampDensity;
  private GenerationType generationType;

  public static GeneratorCommandBuilder create() {
    return new GeneratorCommandBuilder();
  }

  public GeneratorCommandBuilder generatorExecutableFilePath(Path generatorExecutableFile) {
    this.generatorExecutableFile = generatorExecutableFile;
    return this;
  }

  public GeneratorCommandBuilder version(ComparableVersion version) {
    this.version = version;
    return this;
  }

  public GeneratorCommandBuilder mapFilename(String mapFilename) {
    this.mapFilename = mapFilename;
    return this;
  }

  public GeneratorCommandBuilder spawnCount(Integer spawnCount) {
    this.spawnCount = spawnCount;
    return this;
  }

  public GeneratorCommandBuilder mapSize(Integer mapSize) {
    this.mapSize = mapSize;
    return this;
  }

  public GeneratorCommandBuilder seed(String seed) {
    this.seed = seed;
    return this;
  }

  public GeneratorCommandBuilder landDensity(Float landDensity) {
    this.landDensity = landDensity;
    return this;
  }

  public GeneratorCommandBuilder plateauDensity(Float plateauDensity) {
    this.plateauDensity = plateauDensity;
    return this;
  }

  public GeneratorCommandBuilder mountainDensity(Float mountainDensity) {
    this.mountainDensity = mountainDensity;
    return this;
  }

  public GeneratorCommandBuilder rampDensity(Float rampDensity) {
    this.rampDensity = rampDensity;
    return this;
  }

  public GeneratorCommandBuilder generationType(GenerationType generationType) {
    this.generationType = generationType;
    return this;
  }

  public List<String> build() {
    if (generatorExecutableFile == null) {
      throw new IllegalStateException("Map generator path not set");
    }
    if (version.compareTo(new ComparableVersion("1")) >= 0) {
      if (mapFilename == null && (mapSize == null || spawnCount == null || generationType == null)) {
        throw new IllegalStateException("Map generation parameters not properly set");
      }

      List<String> command;

      if (mapFilename == null) {
        command = new ArrayList<>(List.of("java", "-jar", generatorExecutableFile.toAbsolutePath().toString(),
            "--map-size", mapSize.toString(), "--spawn-count", spawnCount.toString()));

        switch (generationType) {
          case BLIND -> command.add("--blind");
          case TOURNAMENT -> command.add("--tournament-style");
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
      } else {
        command = new ArrayList<>(List.of("java", "-jar", generatorExecutableFile.toAbsolutePath().toString(),
            "--map-name", mapFilename));
      }

      return command;
    } else {
      return Arrays.asList("java", "-jar", generatorExecutableFile.toAbsolutePath().toString(), ".",
          String.valueOf(seed), version.toString(), mapFilename);
    }
  }
}
