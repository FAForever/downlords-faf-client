package com.faforever.client.map.generator;

import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
@Slf4j
public class MapGeneratorService {

  /**
   * Naming template for generated maps. It is all lower case because server expects lower case names for maps.
   */
  public static final String GENERATED_MAP_NAME = "neroxis_map_generator_%s_%d";
  private static final String GENERATOR_DEFAULT_VERSION = "0.1.2";

  public static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";
  @VisibleForTesting
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";
  public static final int GENERATION_TIMEOUT_SECONDS = 60;
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d\\d?\\d?\\.\\d\\d?\\d?\\.\\d\\d?\\d?");
  private static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("neroxis_map_generator_(" + VERSION_PATTERN + ")_(-?\\d+)");
  @Getter
  private final Path generatorExecutablePath;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;
  private final TaskService taskService;

  @Getter
  private Path customMapsDirectory;
  private Random seedGenerator;

  @Getter
  private Image generatedMapPreviewImage;

  public MapGeneratorService(ApplicationContext applicationContext, PreferencesService preferencesService, TaskService taskService) {
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;
    this.taskService = taskService;

    generatorExecutablePath = preferencesService.getFafDataDirectory().resolve(GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    if (!Files.exists(generatorExecutablePath)) {
      try {
        Files.createDirectory(generatorExecutablePath);
      } catch (IOException e) {
        log.error("Could not create map generator executable directory.", e);
      }
    }

    seedGenerator = new Random();

    customMapsDirectory = this.preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();

    try {
      generatedMapPreviewImage = new Image(new ClassPathResource("/images/generatedMapIcon.png").getURL().toString(), true);
    } catch (IOException e) {
      log.error("Could not load generated map preview image.", e);
    }
  }

  @PostConstruct
  public void postConstruct() {
    deleteGeneratedMaps();
  }


  private void deleteGeneratedMaps() {
    log.info("Deleting leftover generated maps...");
    if (customMapsDirectory != null && customMapsDirectory.toFile().exists()) {
      try (Stream<Path> listOfMapFiles = Files.list(customMapsDirectory)) {
        listOfMapFiles
            .filter(Files::isDirectory)
            .filter(p -> GENERATED_MAP_PATTERN.matcher(p.getFileName().toString()).matches())
            .forEach(p -> noCatch(() -> FileUtils.deleteRecursively(p)));
      } catch (IOException e) {
        log.error("Could not list custom maps directory for deleting leftover generated maps.", e);
      }
    }
  }


  public CompletableFuture<String> generateMap() {
    return generateMap(GENERATOR_DEFAULT_VERSION, seedGenerator.nextLong());
  }

  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Map name is not a generated map");
    }
    return generateMap(matcher.group(1), Long.parseLong(matcher.group(2)));
  }


  public CompletableFuture<String> generateMap(String version, long seed) {

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, version);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadGeneratorFuture;
    if (!Files.exists(generatorExecutablePath)) {
      if (!VERSION_PATTERN.matcher(version).matches()) {
        log.error("Unsupported generator version: {}", version);
        return CompletableFuture.supplyAsync(() -> {
          throw new RuntimeException("Unsupported generator version: " + version);
        });
      }

      log.info("Downloading MapGenerator version: {}", version);
      DownloadMapGeneratorTask downloadMapGeneratorTask = applicationContext.getBean(DownloadMapGeneratorTask.class);
      downloadMapGeneratorTask.setVersion(version);
      downloadGeneratorFuture = taskService.submitTask(downloadMapGeneratorTask).getFuture();
    } else {
      log.info("Found MapGenerator version: {}", version);
      downloadGeneratorFuture = CompletableFuture.completedFuture(null);
    }

    String mapFilename = String.format(GENERATED_MAP_NAME, version, seed);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(version);
    generateMapTask.setSeed(seed);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);
    generateMapTask.setMapFilename(mapFilename);

    return downloadGeneratorFuture.thenApplyAsync((aVoid) -> {
      CompletableFuture<Void> generateMapFuture = taskService.submitTask(generateMapTask).getFuture();
      generateMapFuture.join();
      return mapFilename;
    });
  }


  public boolean isGeneratedMap(String mapName) {
    return GENERATED_MAP_PATTERN.matcher(mapName).matches();
  }
}
