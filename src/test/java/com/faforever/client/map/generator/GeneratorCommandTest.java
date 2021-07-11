package com.faforever.client.map.generator;

import com.faforever.client.map.generator.GeneratorCommand.GeneratorCommandBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorCommandTest {

  private static final String javaPath = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(org.bridj.Platform.isWindows() ? "java.exe" : "java").toAbsolutePath().toString();

  private static GeneratorCommandBuilder defaultBuilder() {
    return GeneratorCommand.builder()
        .generatorExecutableFile(Path.of("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .mapSize(512)
        .spawnCount(6)
        .numTeams(2)
        .generationType(GenerationType.CASUAL);
  }

  @Test
  public void testDefaultSet() {
    assertNotNull(defaultBuilder().build().getCommand());
    assertEquals(defaultBuilder().build().getCommand(), List.of(javaPath, "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
        "--map-size", "512", "--spawn-count", "6", "--num-teams", "2"));
  }

  @Test
  public void testFilePathNullThrowsException() {
    assertThrows(IllegalStateException.class, () -> defaultBuilder().generatorExecutableFile(null).build().getCommand());
  }

  @Test
  public void testMapSizeNullThrowsException() {
    assertThrows(IllegalStateException.class, () -> defaultBuilder().mapSize(null).build().getCommand());
  }

  @Test
  public void testSpawnCountNullThrowsException() {
    assertThrows(IllegalStateException.class, () -> defaultBuilder().spawnCount(null).build().getCommand());
  }

  @Test
  public void testMapNameNoException() {
    List<String> command = GeneratorCommand.builder().mapFilename("neroxis_map_generator_1.0.0_0")
        .generatorExecutableFile(Paths.get("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .build()
        .getCommand();
    assertTrue(command.containsAll(List.of(javaPath, "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
        "--map-name", "neroxis_map_generator_1.0.0_0")));
  }

  @Test
  public void testLandDensitySet() {
    List<String> command = defaultBuilder().landDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--land-density", "0.1")));
  }

  @Test
  public void testPlateauDensitySet() {
    List<String> command = defaultBuilder().plateauDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--plateau-density", "0.1")));
  }

  @Test
  public void testMountainDensitySet() {
    List<String> command = defaultBuilder().mountainDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--mountain-density", "0.1")));
  }

  @Test
  public void testRampDensitySet() {
    List<String> command = defaultBuilder().rampDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--ramp-density", "0.1")));
  }

  @Test
  public void testMexDensitySet() {
    List<String> command = defaultBuilder().mexDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--mex-density", "0.1")));
  }

  @Test
  public void testReclaimDensitySet() {
    List<String> command = defaultBuilder().reclaimDensity(.1f).build().getCommand();
    assertTrue(command.containsAll(List.of("--reclaim-density", "0.1")));
  }

  @Test
  public void testGenerationTypeNull() {
    List<String> command = defaultBuilder().generationType(null).build().getCommand();
    assertEquals(defaultBuilder().build().getCommand(), command);
  }

  @Test
  public void testBlindType() {
    List<String> command = defaultBuilder().generationType(GenerationType.BLIND).build().getCommand();
    assertTrue(command.contains("--blind"));
  }

  @Test
  public void testTournamentType() {
    List<String> command = defaultBuilder().generationType(GenerationType.TOURNAMENT).build().getCommand();
    assertTrue(command.contains("--tournament-style"));
  }

  @Test
  public void testUnexploredType() {
    List<String> command = defaultBuilder().generationType(GenerationType.UNEXPLORED).build().getCommand();
    assertTrue(command.contains("--unexplored"));
  }

  @Test
  public void testStyleSet() {
    List<String> command = defaultBuilder().style("TEST").build().getCommand();
    assertTrue(command.containsAll(List.of("--style", "TEST")));
  }

  @Test
  public void testBiomeSet() {
    List<String> command = defaultBuilder().biome("TEST").build().getCommand();
    assertTrue(command.containsAll(List.of("--biome", "TEST")));
  }

  @Test
  public void testCommandArgsSet() {
    List<String> command = defaultBuilder().commandLineArgs("--help").build().getCommand();
    assertTrue(command.contains("--help"));
  }

  @Test
  public void testVersion0() {
    List<String> command = defaultBuilder().version(new ComparableVersion("0.1.5"))
        .generatorExecutableFile(Path.of("mapGenerator_0.1.5.jar"))
        .seed("0")
        .mapFilename("neroxis_map_generator_0.1.5_0")
        .build()
        .getCommand();
    assertEquals(command, List.of(javaPath, "-jar", Path.of("mapGenerator_0.1.5.jar").toAbsolutePath().toString(),
        ".", "0", "0.1.5", "neroxis_map_generator_0.1.5_0"));
  }

}
