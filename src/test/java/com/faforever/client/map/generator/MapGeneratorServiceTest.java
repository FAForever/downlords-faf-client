package com.faforever.client.map.generator;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.MapGenerator;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MapGeneratorServiceTest extends ServiceTest {

  private final ComparableVersion versionGeneratorPresent = new ComparableVersion("2.0.0");
  private final ComparableVersion versionNoGeneratorPresent = new ComparableVersion("1.0.0");
  private final ComparableVersion versionGeneratorTooOld = new ComparableVersion("0.1.0");
  private final ComparableVersion versionNoGeneratorTooNew = new ComparableVersion("3.0.0");
  private final Integer spawnCount = 6;
  private final Integer mapSize = 512;
  private final Integer numTeams = 2;
  private final Map<String, Float> optionMap = Map.of("landDensity", .5f, "plateauDensity", .25f,
      "mountainDensity", .125f, "rampDensity", .75f, "mexDensity", .325f, "reclaimDensity", .825f);
  private final long numericalSeed = -123456789;
  private final int minVersion = 1;
  private final int maxVersion = 2;
  private final String seed = Long.toString(numericalSeed);
  private final String testMapNameNoGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionNoGeneratorPresent, seed);
  private final String testMapNameGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionGeneratorPresent, seed);
  private final String testMapNameTooOldVersion = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionGeneratorTooOld, seed);
  private final String testMapNameTooNewVersion = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionNoGeneratorTooNew, seed);
  @TempDir
  public Path vaultBaseDir;
  @TempDir
  public Path fafDataDirectory;

  private MapGeneratorService instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private DownloadMapGeneratorTask downloadMapGeneratorTask;
  @Mock
  private GeneratorOptionsTask generatorOptionsTask;
  @Mock
  private GenerateMapTask generateMapTask;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private MapGenerator mapGenerator;

  @BeforeEach
  public void setUp() throws Exception {
    Files.createDirectories(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY));
    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    Files.createFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    when(preferencesService.getFafDataDirectory()).thenReturn(fafDataDirectory);

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .vaultBaseDirectory(vaultBaseDir)
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(applicationContext.getBean(DownloadMapGeneratorTask.class)).thenReturn(downloadMapGeneratorTask);
    when(applicationContext.getBean(GenerateMapTask.class)).thenReturn(generateMapTask);
    when(applicationContext.getBean(GeneratorOptionsTask.class)).thenReturn(generatorOptionsTask);
    when(clientProperties.getMapGenerator()).thenReturn(mapGenerator);
    when(mapGenerator.getMaxSupportedMajorVersion()).thenReturn(maxVersion);
    when(mapGenerator.getMinSupportedMajorVersion()).thenReturn(minVersion);

    instance = new MapGeneratorService(applicationContext, preferencesService, taskService, clientProperties, WebClient.builder());

    instance.afterPropertiesSet();

    when(downloadMapGeneratorTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(generateMapTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(generatorOptionsTask.getFuture()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>(List.of("TEST"))));
    doAnswer(invocation -> {
      CompletableTask<Void> task = invocation.getArgument(0);
      task.getFuture().get();
      return task;
    }).when(taskService).submitTask(any());
  }

  @Test
  public void testGenerateMapNoGeneratorPresent() {
    instance.generateMap(testMapNameNoGenerator).join();

    verify(taskService).submitTask(downloadMapGeneratorTask);
    verify(downloadMapGeneratorTask).setVersion(versionNoGeneratorPresent.toString());

    //See test below
    verify(taskService).submitTask(generateMapTask);
  }

  @Test
  public void testGenerateMapGeneratorPresent() throws Exception {
    instance.generateMap(testMapNameGenerator).get();

    verify(taskService).submitTask(generateMapTask);

    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setSeed(seed);
    verify(generateMapTask).setMapFilename(testMapNameGenerator);

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    verifyNoMoreInteractions(taskService);
  }

  @Test
  public void testWrongMapNameThrowsException() {
    CompletableFuture<String> future = instance.generateMap("neroxis_no_map");
    assertThat(assertThrows(CompletionException.class, future::join).getCause(), IsInstanceOf.instanceOf(InvalidParameterException.class));
  }

  @Test
  public void testTooNewVersionThrowsException() {
    CompletableFuture<String> future = instance.generateMap(testMapNameTooNewVersion);
    assertThat(assertThrows(CompletionException.class, future::join).getCause(), IsInstanceOf.instanceOf(UnsupportedVersionException.class));
  }

  @Test
  public void testTooOldVersionThrowsException() {
    CompletableFuture<String> future = instance.generateMap(testMapNameTooOldVersion);
    assertThat(assertThrows(CompletionException.class, future::join).getCause(), IsInstanceOf.instanceOf(OutdatedVersionException.class));
  }

  @Test
  public void testGenerateMapOptionMap() {
    instance.setGeneratorVersion(versionGeneratorPresent);
    CompletableFuture<String> future = instance.generateMap(spawnCount, mapSize, numTeams, optionMap, GenerationType.CASUAL);
    future.join();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setSpawnCount(spawnCount);
    verify(generateMapTask).setMapSize(mapSize);
    verify(generateMapTask).setNumTeams(numTeams);
    verify(generateMapTask).setLandDensity(optionMap.get("landDensity"));
    verify(generateMapTask).setMountainDensity(optionMap.get("mountainDensity"));
    verify(generateMapTask).setPlateauDensity(optionMap.get("plateauDensity"));
    verify(generateMapTask).setRampDensity(optionMap.get("rampDensity"));
    verify(generateMapTask).setMexDensity(optionMap.get("mexDensity"));
    verify(generateMapTask).setReclaimDensity(optionMap.get("reclaimDensity"));
    verify(generateMapTask).setGenerationType(GenerationType.CASUAL);
  }

  @Test
  public void testGenerateMapStyle() {
    instance.setGeneratorVersion(versionGeneratorPresent);
    CompletableFuture<String> future = instance.generateMap(spawnCount, mapSize, numTeams, "TEST");
    future.join();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setSpawnCount(spawnCount);
    verify(generateMapTask).setMapSize(mapSize);
    verify(generateMapTask).setNumTeams(numTeams);
    verify(generateMapTask).setStyle("TEST");
  }

  @Test
  public void testGenerateMapWithArgs() {
    instance.setGeneratorVersion(versionGeneratorPresent);
    CompletableFuture<String> future = instance.generateMapWithArgs("--help");
    future.join();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setCommandLineArgs("--help");
  }

  @Test
  public void testGetStyles() {
    instance.setGeneratorVersion(versionGeneratorPresent);
    CompletableFuture<List<String>> future = instance.getGeneratorStyles();
    future.join();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generatorOptionsTask).setGeneratorExecutableFile(fafDataDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generatorOptionsTask).setVersion(versionGeneratorPresent);
    verify(generatorOptionsTask).setQuery("--styles");
  }
}
