package com.faforever.client.map.generator;

import com.faforever.client.map.generator.GeneratorCommand.GeneratorCommandBuilder;
import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.not;
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

  private static GeneratorCommandBuilder customStyleBuilder() {
    return defaultBuilder()
        .terrainStyle("terrain")
        .textureStyle("texture")
        .resourceStyle("resource")
        .propStyle("prop");
  }

  private static GeneratorCommandBuilder maximumArgsBuilder() {
    return customStyleBuilder()
        .seed("100")
        .symmetry("XZ");
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
  public void testSeedSet() {
    List<String> command = defaultBuilder().seed("100").build().getCommand();
    assertThat(command, containsInRelativeOrder("--seed", "100"));
  }

  @Test
  public void testSymmetrySet() {
    List<String> command = defaultBuilder().symmetry("XZ").build().getCommand();
    assertThat(command, containsInRelativeOrder("--terrain-symmetry", "XZ"));
  }

  @Test
  public void testTerrainStyleSet() {
    List<String> command = defaultBuilder().terrainStyle("TERRAIN").build().getCommand();
    assertThat(command, containsInRelativeOrder("--terrain-style", "TERRAIN"));
  }

  @Test
  public void testTextureStyleSet() {
    List<String> command = defaultBuilder().textureStyle("BIOME").build().getCommand();
    assertThat(command, containsInRelativeOrder("--texture-style", "BIOME"));
  }

  @Test
  public void testResourceStyleSet() {
    List<String> command = defaultBuilder().resourceStyle("RESOURCE").build().getCommand();
    assertThat(command, containsInRelativeOrder("--resource-style", "RESOURCE"));
  }

  @Test
  public void testPropStyleSet() {
    List<String> command = defaultBuilder().propStyle("PROPS").build().getCommand();
    assertThat(command, containsInRelativeOrder("--prop-style", "PROPS"));
  }

  @Test
  public void testResourceDensitySet() {
    List<String> command = defaultBuilder().resourceDensity(.5f).build().getCommand();
    assertThat(command, containsInRelativeOrder("--resource-density", "0.5"));
  }

  @Test
  public void testReclaimDensitySet() {
    List<String> command = defaultBuilder().reclaimDensity(.5f).build().getCommand();
    assertThat(command, containsInRelativeOrder("--reclaim-density", "0.5"));
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
    assertThat(command, containsInRelativeOrder("--style", "TEST"));
  }

  @Test
  public void testRandomSymmetrySet() {
    List<String> command = defaultBuilder().symmetry("RANDOM").build().getCommand();
    assertThat(command, not(contains("--terrain-symmetry")));
  }

  @Test
  public void testRandomStyleSet() {
    List<String> command = defaultBuilder().style("RANDOM").build().getCommand();
    assertThat(command, not(contains("--style")));
  }

  @Test
  public void testRandomTerrainStyleSet() {
    List<String> command = defaultBuilder().terrainStyle("RANDOM").build().getCommand();
    assertThat(command, not(contains("--terrain-style")));
  }

  @Test
  public void testRandomTextureStyleSet() {
    List<String> command = defaultBuilder().textureStyle("RANDOM").build().getCommand();
    assertThat(command, not(contains("--texture-style")));
  }

  @Test
  public void testRandomResourceStyleSet() {
    List<String> command = defaultBuilder().resourceStyle("RANDOM").build().getCommand();
    assertThat(command, not(contains("--resource-style")));
  }

  @Test
  public void testRandomPropStyleSet() {
    List<String> command = defaultBuilder().propStyle("RANDOM").build().getCommand();
    assertThat(command, not(contains("--prop-style")));
  }

  @Test
  public void testCommandArgsSet() {
    List<String> command = defaultBuilder().commandLineArgs("--help").build().getCommand();
    assertTrue(command.contains("--help"));
  }

  @Test
  public void testStyleRemovesCustomStyleArgs() {
    List<String> command = customStyleBuilder().style("test").build().getCommand();
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
  }

  @Test
  public void testMapNameRemovesArgs() {
    List<String> command = maximumArgsBuilder().mapName("test").build().getCommand();
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
    assertFalse(command.contains("--seed"));
    assertFalse(command.contains("--symmetry"));
    assertFalse(command.contains("--spawn-count"));
    assertFalse(command.contains("--map-size"));
    assertFalse(command.contains("--num-teams"));
  }

  @Test
  public void testCommandLineRemovesArgs() {
    // With commandLineArgs, only command line args and numToGenerate are added
    // Other parameters (style, seed, symmetry, etc.) are ignored
    List<String> command = maximumArgsBuilder().commandLineArgs("--test").build().getCommand();

    // commandLineArgs is added
    assertTrue(command.contains("--test"));

    // numToGenerate is not set, so it should not be added
    assertFalse(command.contains("--num-to-generate"));

    // Other parameters are ignored when commandLineArgs is set
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
    assertFalse(command.contains("--seed"));
    assertFalse(command.contains("--symmetry"));
    assertFalse(command.contains("--spawn-count"));
    assertFalse(command.contains("--map-size"));
    assertFalse(command.contains("--num-teams"));
  }

  @Test
  public void testNonCasualRemovesCustomizationArgs() {
    List<String> command = maximumArgsBuilder().generationType(GenerationType.BLIND).build().getCommand();
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
    assertFalse(command.contains("--seed"));
    assertFalse(command.contains("--symmetry"));
    assertTrue(command.contains("--spawn-count"));
    assertTrue(command.contains("--map-size"));
    assertTrue(command.contains("--num-teams"));

    command = maximumArgsBuilder().generationType(GenerationType.TOURNAMENT).build().getCommand();
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
    assertFalse(command.contains("--seed"));
    assertFalse(command.contains("--symmetry"));
    assertTrue(command.contains("--spawn-count"));
    assertTrue(command.contains("--map-size"));
    assertTrue(command.contains("--num-teams"));

    command = maximumArgsBuilder().generationType(GenerationType.UNEXPLORED).build().getCommand();
    assertFalse(command.contains("--terrain-style"));
    assertFalse(command.contains("--texture-style"));
    assertFalse(command.contains("--resource-style"));
    assertFalse(command.contains("--prop-style"));
    assertFalse(command.contains("--seed"));
    assertFalse(command.contains("--symmetry"));
    assertTrue(command.contains("--spawn-count"));
    assertTrue(command.contains("--map-size"));
    assertTrue(command.contains("--num-teams"));
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

  @Test
  public void testNumToGenerate() {
    List<String> command = defaultBuilder().version(new ComparableVersion("1.0.0"))
                                           .numToGenerate(3)
                                           .build()
                                           .getCommand();
    assertTrue(command.contains("--num-to-generate"));
    assertTrue(command.contains("3"));
  }

  @Test
  public void testNumToGenerateWithGeneratorOptions() {
    GeneratorOptions generatorOptions = GeneratorOptions.builder()
                                                        .spawnCount(6)
                                                        .numTeams(2)
                                                        .mapSize(512)
                                                        .seed("100")
                                                        .generationType(GenerationType.CASUAL)
                                                        .build();

    List<String> command = GeneratorCommand.builder()
                                           .javaExecutable(JAVA_PATH)
                                           .generatorExecutableFile(Path.of("mapGenerator_1.0.0.jar"))
                                           .version(new ComparableVersion("1.0.0"))
                                           .generatorOptions(generatorOptions)
                                           .numToGenerate(3)
                                           .build()
                                           .getCommand();

    assertTrue(command.contains("--num-to-generate"));
    assertTrue(command.contains("3"));
    assertTrue(command.contains("--map-size"));
    assertTrue(command.contains("512"));
    assertTrue(command.contains("--spawn-count"));
    assertTrue(command.contains("6"));
    assertTrue(command.contains("--num-teams"));
    assertTrue(command.contains("2"));
  }

}
