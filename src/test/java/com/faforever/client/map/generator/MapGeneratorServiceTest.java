package com.faforever.client.map.generator;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MapGeneratorServiceTest extends AbstractPlainJavaFxTest {

  private final String versionGeneratorPresent = "1.0.0";
  private final String versionNoGeneratorPresent = "0.0.0";
  private final String unsupportedVersion = "2.0";
  private final long seed = -123456789;
  private final String testMapNameNoGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionNoGeneratorPresent, seed);
  private final String testMapNameGenerator = String.format(MapGeneratorService.GENERATED_MAP_NAME, versionGeneratorPresent, seed);
  private final String testMapNameUnsupportedVersion = String.format(MapGeneratorService.GENERATED_MAP_NAME, unsupportedVersion, seed);
  @Rule
  public TemporaryFolder vaultBaseDirectory = new TemporaryFolder();
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

  @Before
  public void setUp() throws IOException {
    Path customMapsDir = vaultBaseDirectory.getRoot().toPath().resolve("maps");
    Files.createDirectories(customMapsDir.resolve(testMapNameGenerator));//will be deleted on startup

    fafDataDirectory.newFolder(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    String generatorExecutableName = String.format(MapGeneratorService.GENERATOR_EXECUTABLE_FILENAME, versionGeneratorPresent);
    Files.createFile(fafDataDirectory.getRoot().toPath().resolve(MapGeneratorService.GENERATOR_EXECUTABLE_SUB_DIRECTORY).resolve(generatorExecutableName));

    when(preferencesService.getFafDataDirectory()).thenReturn(fafDataDirectory.getRoot().toPath());

    Preferences preferences = new Preferences();
    preferences.getForgedAlliance().setVaultBaseDirectory(vaultBaseDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance = new MapGeneratorService(applicationContext, preferencesService, taskService);

    instance.postConstruct();
    Stream<Path> list = Files.list(customMapsDir);
    assertThat(list.collect(Collectors.toList()), not(contains(testMapNameGenerator)));
    list.close();

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
    when(applicationContext.getBean(DownloadMapGeneratorTask.class)).thenReturn(downloadMapGeneratorTask);
    when(applicationContext.getBean(GenerateMapTask.class)).thenReturn(generateMapTask);

    instance.generateMap(testMapNameNoGenerator).join();

    verify(taskService).submitTask(downloadMapGeneratorTask);
    verify(downloadMapGeneratorTask).setVersion(versionNoGeneratorPresent);

    //See test below
    verify(taskService).submitTask(generateMapTask);
  }

  @Test
  public void testGenerateMapGeneratorPresent() throws Exception {
    when(applicationContext.getBean(GenerateMapTask.class)).thenReturn(generateMapTask);

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
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(startsWith("Map name is not a generated map"));
    CompletableFuture<String> future = instance.generateMap(testMapNameUnsupportedVersion);
    future.join();
  }

  @Test
  public void testWrongVersionThrowsException() {
    expectedException.expect(CompletionException.class);

    CompletableFuture<String> future = instance.generateMap(unsupportedVersion, seed);
    future.join();
  }
}
