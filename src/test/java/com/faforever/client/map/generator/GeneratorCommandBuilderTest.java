package com.faforever.client.map.generator;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GeneratorCommandBuilderTest {

  private static GeneratorCommandBuilder defaultBuilder() {
    return GeneratorCommandBuilder.create()
        .generatorExecutableFilePath(Path.of("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .mapSize(512)
        .spawnCount(6)
        .generationType(GenerationType.CASUAL);
  }

  @Test
  public void testDefaultSet() throws Exception {
    assertNotNull(defaultBuilder().build());
    assertEquals(defaultBuilder().build(), List.of("java", "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
        "--map-size", "512", "--spawn-count", "6"));
  }

  @Test(expected = IllegalStateException.class)
  public void testFilePathNullThrowsException() throws Exception {
    defaultBuilder().generatorExecutableFilePath(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testMapSizeNullThrowsException() throws Exception {
    defaultBuilder().mapSize(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testSpawnCountNullThrowsException() throws Exception {
    defaultBuilder().spawnCount(null).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testGenerationTypeNullThrowsException() throws Exception {
    defaultBuilder().generationType(null).build();
  }

  @Test
  public void testMapNameNoException() throws Exception {
    List<String> command = new GeneratorCommandBuilder().mapFilename("neroxis_map_generator_1.0.0_0")
        .generatorExecutableFilePath(Paths.get("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .build();
    assertTrue(command.containsAll(List.of("java", "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
        "--map-name", "neroxis_map_generator_1.0.0_0")));
  }

  @Test
  public void testLandDensitySet() throws Exception {
    List<String> command = defaultBuilder().landDensity(.1f).build();
    assertTrue(command.containsAll(List.of("--land-density", "0.1")));
  }

  @Test
  public void testPlateauDensitySet() throws Exception {
    List<String> command = defaultBuilder().plateauDensity(.1f).build();
    assertTrue(command.containsAll(List.of("--plateau-density", "0.1")));
  }

  @Test
  public void testMountainDensitySet() throws Exception {
    List<String> command = defaultBuilder().mountainDensity(.1f).build();
    assertTrue(command.containsAll(List.of("--mountain-density", "0.1")));
  }

  @Test
  public void testRampDensitySet() throws Exception {
    List<String> command = defaultBuilder().rampDensity(.1f).build();
    assertTrue(command.containsAll(List.of("--ramp-density", "0.1")));
  }

  @Test
  public void testBlindType() throws Exception {
    List<String> command = defaultBuilder().generationType(GenerationType.BLIND).build();
    assertTrue(command.contains("--blind"));
  }

  @Test
  public void testTournamentType() throws Exception {
    List<String> command = defaultBuilder().generationType(GenerationType.TOURNAMENT).build();
    assertTrue(command.contains("--tournament-style"));
  }

  @Test
  public void testVersion0() throws Exception {
    List<String> command = defaultBuilder().version(new ComparableVersion("0.1.5"))
        .generatorExecutableFilePath(Path.of("mapGenerator_0.1.5.jar"))
        .seed("0")
        .mapFilename("neroxis_map_generator_0.1.5_0")
        .build();
    assertEquals(command, List.of("java", "-jar", Path.of("mapGenerator_0.1.5.jar").toAbsolutePath().toString(),
        ".", "0", "0.1.5", "neroxis_map_generator_0.1.5_0"));
  }

}
