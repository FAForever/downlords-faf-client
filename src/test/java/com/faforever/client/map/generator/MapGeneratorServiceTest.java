package com.faforever.client.map.generator;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.MapGenerator;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MapGeneratorServiceTest extends AbstractPlainJavaFxTest {

  private final String generatedMapFormat = "neroxis_map_generator_%s_%s";
  private final String versionGeneratorPresent = "2.0.0";
  private final String versionNoGeneratorPresent = "1.0.0";
  private final String versionGeneratorTooOld = "0.1.0";
  private final String versionNoGeneratorTooNew = "3.0.0";
  private final String unsupportedVersion = "3.0";
  private final byte[] optionArray = {6,0,0,0,0};
  private final long numericalSeed = -123456789;
  private final String optionString = MapGeneratorService.NAME_ENCODER.encode(optionArray);
  private final int minVersion = 1;
  private final int maxVersion = 2;
  private final String seedAndOptions = MapGeneratorService.NAME_ENCODER.encode(ByteBuffer.allocate(8).putLong(numericalSeed).array());
  private final String seed = Long.toString(numericalSeed);
  private final String testMapNameNoGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionNoGeneratorPresent, seed);
  private final String testMapNameGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionGeneratorPresent, seed);
  private final String testMapNameUnsupportedVersion = String.format(MapGeneratorService.GENERATED_MAP_NAME, unsupportedVersion, seed);
  @Rule
  public TemporaryFolder vaultBaseDir = new TemporaryFolder();
  @Rule
  public TemporaryFolder fafDataDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
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
  private GenerateMapTask generateMapTask;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private MapGenerator mapGenerator;
  @Mock
  private GithubGeneratorRelease release;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private ResponseEntity<List<GithubGeneratorRelease>> responseEntity;

  @Before
  public void setUp() throws Exception {
    fafDataDirectory.newFolder(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    Files.createFile(fafDataDirectory.getRoot().toPath().resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    when(preferencesService.getFafDataDirectory()).thenReturn(fafDataDirectory.getRoot().toPath());

    Preferences preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    preferences.getForgedAlliance().setVaultBaseDirectory(Paths.get(vaultBaseDir.getRoot().getAbsolutePath()));

    when(applicationContext.getBean(DownloadMapGeneratorTask.class)).thenReturn(downloadMapGeneratorTask);
    when(applicationContext.getBean(GenerateMapTask.class)).thenReturn(generateMapTask);
    when(clientProperties.getMapGenerator()).thenReturn(mapGenerator);
    when(mapGenerator.getMaxSupportedMajorVersion()).thenReturn(maxVersion);
    when(mapGenerator.getMinSupportedMajorVersion()).thenReturn(minVersion);

    instance = new MapGeneratorService(applicationContext, preferencesService, taskService, clientProperties);

    instance.afterPropertiesSet();

    when(downloadMapGeneratorTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    when(generateMapTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      CompletableTask<Void> task = invocation.getArgument(0);
      task.getFuture().get();
      return task;
    }).when(taskService).submitTask(any());
  }

  @Test
  public void testGenerateMapNoGeneratorPresent() {
    instance.generateMap(testMapNameNoGenerator).join();

    verify(taskService).submitTask(downloadMapGeneratorTask);
    verify(downloadMapGeneratorTask).setVersion(versionNoGeneratorPresent);

    //See test below
    verify(taskService).submitTask(generateMapTask);
  }

  @Test
  public void testGenerateMapGeneratorPresent() throws Exception {
    String mapName = instance.generateMap(testMapNameGenerator).get();
    assertThat(mapName, equalTo(testMapNameGenerator));

    verify(taskService).submitTask(generateMapTask);

    verify(generateMapTask).setVersion(versionGeneratorPresent);
    verify(generateMapTask).setSeed(seed);
    verify(generateMapTask).setMapFilename(testMapNameGenerator);

    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    verify(generateMapTask).setGeneratorExecutableFile(fafDataDirectory.getRoot().toPath().resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    verifyNoMoreInteractions(taskService);
  }

  @Test
  public void testWrongMapNameThrowsException() {
    expectedException.expect(CompletionException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(InvalidParameterException.class));

    CompletableFuture<String> future = instance.generateMap(testMapNameUnsupportedVersion);
    future.join();
  }

  @Test
  public void testWrongVersionThrowsException() {
    expectedException.expect(CompletionException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(UnsupportedVersionException.class));

    CompletableFuture<String> future = instance.generateMap(unsupportedVersion, seed);
    future.join();
  }

  @Test
  public void testTooNewVersionThrowsException() {
    expectedException.expect(CompletionException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(UnsupportedVersionException.class));

    CompletableFuture<String> future = instance.generateMap(versionNoGeneratorTooNew, seed);
    future.join();
  }

  @Test
  public void testTooOldVersionThrowsException() {
    expectedException.expect(CompletionException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(OutdatedVersionException.class));

    CompletableFuture<String> future = instance.generateMap(versionGeneratorTooOld, seed);
    future.join();
  }

  @Test
  public void testGenerateMapNoVersionSet() {
    expectedException.expect(NullPointerException.class);
    CompletableFuture<String> future = instance.generateMap();
    future.join();
  }

  @Test
  public void testGenerateMapNoInputs() {
    instance.setGeneratorVersion(new ComparableVersion(versionGeneratorPresent));
    CompletableFuture<String> future = instance.generateMap();
    future.join();

    verify(generateMapTask).setMapFilename(matches(String.format(generatedMapFormat,versionGeneratorPresent,".*")));
  }

  @Test
  public void testGenerateMapOptionArray() {
    instance.setGeneratorVersion(new ComparableVersion(versionGeneratorPresent));
    CompletableFuture<String> future = instance.generateMap(optionArray);
    future.join();
  }

  @Test
  public void testGenerateMapStringAndOptionArray() {
    instance.setGeneratorVersion(new ComparableVersion(versionGeneratorPresent));
    CompletableFuture<String> future = instance.generateMap(versionGeneratorPresent,optionArray);
    future.join();

    verify(generateMapTask).setMapFilename(matches(String.format(generatedMapFormat,versionGeneratorPresent,".*_"+optionString)));
  }

  @Test
  public void testGenerateMapComparableAndOptionArray() {
    instance.setGeneratorVersion(new ComparableVersion(versionGeneratorPresent));
    CompletableFuture<String> future = instance.generateMap(new ComparableVersion(versionGeneratorPresent),optionArray);
    future.join();

    verify(generateMapTask).setMapFilename(matches(String.format(generatedMapFormat,versionGeneratorPresent,".*_"+optionString)));
  }

  @Test
  public void testNumericSeedNoOption() {
    CompletableFuture<String> future = instance.generateMap(versionGeneratorPresent, seed);
    future.join();

    verify(generateMapTask).setMapFilename(String.format(generatedMapFormat,versionGeneratorPresent,seed));
  }

  @Test
  public void testBase64SeedAndOption() {
    CompletableFuture<String> future = instance.generateMap(versionGeneratorPresent, seedAndOptions);
    future.join();

    verify(generateMapTask).setMapFilename(String.format(generatedMapFormat,versionGeneratorPresent,seedAndOptions));
  }

  @Test
  public void testGeneratorVersion0() {
    when(mapGenerator.getMinSupportedMajorVersion()).thenReturn(0);
    CompletableFuture<String> future = instance.generateMap(versionGeneratorTooOld, seed);
    future.join();

    verify(generateMapTask).setSeed(seed);
    verify(generateMapTask).setMapFilename(String.format(generatedMapFormat,versionGeneratorTooOld,seed));
  }

  @Test
  public void testGeneratorVersion0WithOptions() {
    when(mapGenerator.getMinSupportedMajorVersion()).thenReturn(0);
    CompletableFuture<String> future = instance.generateMap(versionGeneratorTooOld, seedAndOptions);
    future.join();

    verify(generateMapTask).setSeed(seed);
    verify(generateMapTask).setMapFilename(String.format(generatedMapFormat,versionGeneratorTooOld,seed));
  }

}
