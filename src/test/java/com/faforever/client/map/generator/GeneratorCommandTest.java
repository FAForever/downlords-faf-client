package com.faforever.client.map.generator;

import com.faforever.client.map.generator.GeneratorCommand.GeneratorCommandBuilder;
import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorCommandTest extends ServiceTest {

  private static final Path JAVA_PATH = Path.of("java");

  private static GeneratorCommandBuilder defaultBuilder() {
    return GeneratorCommand.builder()
        .javaExecutable(JAVA_PATH)
        .generatorExecutableFile(Path.of("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .mapSize(512)
        .spawnCount(6)
        .numTeams(2)
        .generationType(GenerationType.CASUAL);
  }

  private static GeneratorCommandBuilder densityBuilder() {
    return defaultBuilder()
        .biome("biome")
        .reclaimDensity(1f)
        .mexDensity(1f)
        .rampDensity(1f)
        .plateauDensity(1f)
        .mountainDensity(1f)
        .landDensity(1f);
  }

  @Test
  public void testDefaultSet() {
    assertNotNull(defaultBuilder().build().getCommand());
    assertEquals(defaultBuilder().build().getCommand(), List.of(JAVA_PATH.toAbsolutePath().toString(), "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
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
    List<String> command = GeneratorCommand.builder().mapName("neroxis_map_generator_1.0.0_0")
        .javaExecutable(Path.of("java"))
        .generatorExecutableFile(Path.of("mapGenerator_1.0.0.jar"))
        .version(new ComparableVersion("1.0.0"))
        .build()
        .getCommand();
    assertTrue(command.containsAll(List.of(JAVA_PATH.toAbsolutePath().toString(), "-jar", Path.of("mapGenerator_1.0.0.jar").toAbsolutePath().toString(),
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
  public void testStyleRemovesDensityArgs() {
    List<String> command = densityBuilder().style("test").build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));
  }

  @Test
  public void testMapNameRemovesArgs() {
    List<String> command = densityBuilder().mapName("test").build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));
    assertFalse(command.contains("--spawn-count"));
    assertFalse(command.contains("--map-size"));
    assertFalse(command.contains("--num-teams"));
  }

  @Test
  public void testCommandLineRemovesArgs() {
    List<String> command = densityBuilder().commandLineArgs("--test").build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));
    assertFalse(command.contains("--spawn-count"));
    assertFalse(command.contains("--map-size"));
    assertFalse(command.contains("--num-teams"));
  }

  @Test
  public void testNonCasualRemovesDensityArgs() {
    List<String> command = densityBuilder().generationType(GenerationType.BLIND).build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));

    command = densityBuilder().generationType(GenerationType.TOURNAMENT).build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));

    command = densityBuilder().generationType(GenerationType.UNEXPLORED).build().getCommand();
    assertFalse(command.contains("--reclaim-density"));
    assertFalse(command.contains("--mex-density"));
    assertFalse(command.contains("--land-density"));
    assertFalse(command.contains("--plateau-density"));
    assertFalse(command.contains("--mountain-density"));
    assertFalse(command.contains("--ramp-density"));
  }

  @Test
  public void testVersion0() {
    List<String> command = defaultBuilder().version(new ComparableVersion("0.1.5"))
        .generatorExecutableFile(Path.of("mapGenerator_0.1.5.jar"))
        .seed("0")
        .mapName("neroxis_map_generator_0.1.5_0")
        .build()
        .getCommand();
    assertEquals(command, List.of(JAVA_PATH.toAbsolutePath().toString(), "-jar", Path.of("mapGenerator_0.1.5.jar").toAbsolutePath().toString(),
        ".", "0", "0.1.5", "neroxis_map_generator_0.1.5_0"));
  }

}
