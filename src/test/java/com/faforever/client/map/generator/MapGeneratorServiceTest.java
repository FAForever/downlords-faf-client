package com.faforever.client.map.generator;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.MapGenerator;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.ServiceTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
  public Path tempDirectory;

  private MapGeneratorService instance;

  @Mock
  private TaskService taskService;
  @Mock
  private DownloadMapGeneratorTask downloadMapGeneratorTask;
  @Mock
  private GeneratorOptionsTask generatorOptionsTask;
  @Mock
  private GenerateMapTask generateMapTask;
  @Mock
  private GenerateMultipleMapsTask generateMultipleMapsTask;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private MapGenerator mapGenerator;
  @Mock
  private ObjectFactory<GenerateMapTask> generateMapTaskFactory;
  @Mock
  private ObjectFactory<DownloadMapGeneratorTask> downloadMapGeneratorTaskFactory;
  @Mock
  private ObjectFactory<GeneratorOptionsTask> generatorOptionsTaskFactory;
  @Mock
  private ObjectFactory<GenerateMultipleMapsTask> generateMultipleMapsTaskFactory;

  @BeforeEach
  public void setUp() throws Exception {
    ForgedAlliancePrefs forgedAlliancePrefs = new ForgedAlliancePrefs();
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);

    DataPrefs dataPrefs = new DataPrefs();
    dataPrefs.setBaseDataDirectory(tempDirectory);

    Files.createDirectories(tempDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY));
    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    Files.createFile(tempDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    lenient().when(downloadMapGeneratorTaskFactory.getObject()).thenReturn(downloadMapGeneratorTask);
    lenient().when(generateMapTaskFactory.getObject()).thenReturn(generateMapTask);
    lenient().when(generatorOptionsTaskFactory.getObject()).thenReturn(generatorOptionsTask);
    lenient().when(generateMultipleMapsTaskFactory.getObject()).thenReturn(generateMultipleMapsTask);
    lenient().when(clientProperties.getMapGenerator()).thenReturn(mapGenerator);
    lenient().when(mapGenerator.getMaxSupportedMajorVersion()).thenReturn(maxVersion);
    lenient().when(mapGenerator.getMinSupportedMajorVersion()).thenReturn(minVersion);

    instance = new MapGeneratorService(taskService, clientProperties, forgedAlliancePrefs, dataPrefs, WebClient.builder()
                                                                                                               .build(),
                                       generateMapTaskFactory, downloadMapGeneratorTaskFactory,
                                       generatorOptionsTaskFactory, generateMultipleMapsTaskFactory);

    lenient().when(downloadMapGeneratorTask.getMono()).thenReturn(Mono.empty());
    // Make generateMapTask return a successful result with map name
    lenient().doAnswer(invocation -> Mono.just("neroxis_map_generator_2.0.0_123456789")).when(generateMapTask).getMono();
    lenient().when(generatorOptionsTask.getMono()).thenReturn(Mono.just(new ArrayList<>(List.of("TEST"))));
    lenient().doAnswer(invocation -> {
      CompletableTask<Void> task = invocation.getArgument(0);
      task.getMono().block();
      return task;
    }).when(taskService).submitTask(any());
  }

  @Test
  public void testGenerateMapNoGeneratorPresent() {
    StepVerifier.create(instance.generateMap(testMapNameNoGenerator))
        .expectNext("neroxis_map_generator_2.0.0_123456789")
        .verifyComplete();

    verify(taskService).submitTask(downloadMapGeneratorTask);
    verify(downloadMapGeneratorTask).setVersion(versionNoGeneratorPresent);

    //See test below
    verify(taskService).submitTask(generateMapTask);
  }

  @Test
  public void testGenerateMapGeneratorPresent() throws Exception {
    StepVerifier.create(instance.generateMap(testMapNameGenerator))
        .expectNext("neroxis_map_generator_2.0.0_123456789")
        .verifyComplete();

    verify(taskService).submitTask(generateMapTask);

    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setMapName(testMapNameGenerator);

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(tempDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    verifyNoMoreInteractions(taskService);
  }

  @Test
  public void testWrongMapNameThrowsException() {
    StepVerifier.create(instance.generateMap("neroxis_no_map")).verifyError(InvalidParameterException.class);
  }

  @Test
  public void testTooNewVersionThrowsException() {
    StepVerifier.create(instance.generateMap(testMapNameTooNewVersion)).verifyError(UnsupportedVersionException.class);
  }

  @Test
  public void testTooOldVersionThrowsException() {
    StepVerifier.create(instance.generateMap(testMapNameTooOldVersion)).verifyError(OutdatedVersionException.class);
  }

  @Test
  public void testGenerateMapWithGeneratorOptions() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions generatorOptions = GeneratorOptions.builder().build();
    StepVerifier.create(instance.generateMap(generatorOptions))
        .expectNext("neroxis_map_generator_2.0.0_123456789")
        .verifyComplete();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(tempDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setGeneratorOptions(generatorOptions);
    verify(generateMapTask, never()).setMapName(anyString());
  }

  @Test
  public void testGetStyles() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    StepVerifier.create(instance.getGeneratorStyles()).expectNext(List.of("TEST")).verifyComplete();

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generatorOptionsTask).setGeneratorExecutableFile(tempDirectory.resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));
    verify(generatorOptionsTask).setVersion(versionGeneratorPresent);
    verify(generatorOptionsTask).setQuery("--styles");
  }

  @Test
  public void testGenerateMultipleMapsWithValidInputs() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions generatorOptions = GeneratorOptions.builder()
        .spawnCount(spawnCount)
        .numTeams(numTeams)
        .mapSize(mapSize)
        .seed(seed)
        .build();

    Long seed = 123L;

    StepVerifier.create(instance.generateMultipleMaps(generatorOptions, 3, seed))
                .expectNextMatches(results -> results.size() == 1)
        .verifyComplete();
  }

  @Test
  public void testGenerateMultipleMapsWithNullBaseOptions() {
    Long seed = 123L;

    StepVerifier.create(instance.generateMultipleMaps(null, 1, seed))
        .expectError(IllegalStateException.class)
        .verify();
  }

  @Test
  public void testGenerateMultipleMapsWithNullSeed() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions generatorOptions = GeneratorOptions.builder().build();
    
    StepVerifier.create(instance.generateMultipleMaps(generatorOptions, 1, null))
        .expectError(IllegalStateException.class)
        .verify();
  }


  @Test
  public void testCreateOptionsWithSeed() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions baseOptions = GeneratorOptions.builder()
        .spawnCount(spawnCount)
        .numTeams(numTeams)
        .mapSize(mapSize)
        .seed("originalSeed")
        .generationType(GenerationType.CASUAL)
        .symmetry("Horizontal")
        .style("Urban")
        .terrainStyle("Grass")
        .textureStyle("Normal")
        .resourceStyle("High")
        .propStyle("None")
        .reclaimDensity(0.5f)
        .resourceDensity(0.7f)
        .commandLineArgs("")
        .build();
    
    Long newSeed = 999L;
    
    // Use reflection to access the private method
    MapGeneratorService service = instance;
    GeneratorOptions result = null;
    try {
      var method = MapGeneratorService.class.getDeclaredMethod("createOptionsWithSeed", GeneratorOptions.class, Long.class);
      method.setAccessible(true);
      result = (GeneratorOptions) method.invoke(service, baseOptions, newSeed);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    // Verify all fields are the same except seed
    assert result.spawnCount().equals(spawnCount);
    assert result.numTeams().equals(numTeams);
    assert result.mapSize().equals(mapSize);
    assert result.seed().equals(Long.toString(newSeed));
    assert result.generationType() == GenerationType.CASUAL;
    assert result.symmetry().equals("Horizontal");
    assert result.style().equals("Urban");
    assert result.terrainStyle().equals("Grass");
    assert result.textureStyle().equals("Normal");
    assert result.resourceStyle().equals("High");
    assert result.propStyle().equals("None");
    assert result.reclaimDensity().equals(0.5f);
    assert result.resourceDensity().equals(0.7f);
    assert result.commandLineArgs().equals("");
  }

  @Test
  public void testGenerateMultipleMapsWithResultsNullBaseOptions() {
    StepVerifier.create(instance.generateMultipleMapsWithResults(null, 3, 123L))
                .expectError(IllegalStateException.class)
                .verify();
  }

  @Test
  public void testGenerateMultipleMapsWithResultsNullSeed() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions generatorOptions = GeneratorOptions.builder().build();

    StepVerifier.create(instance.generateMultipleMapsWithResults(generatorOptions, 3, null))
                .expectError(IllegalStateException.class)
                .verify();
  }

  @Test
  public void testGenerateMultipleMapsWithResultsNegativeMapCount() {
    ReflectionTestUtils.setField(instance, "defaultGeneratorVersion", versionGeneratorPresent);
    GeneratorOptions generatorOptions = GeneratorOptions.builder().build();

    StepVerifier.create(instance.generateMultipleMapsWithResults(generatorOptions, 0, 123L))
                .expectError(IllegalArgumentException.class)
                .verify();
  }

}
