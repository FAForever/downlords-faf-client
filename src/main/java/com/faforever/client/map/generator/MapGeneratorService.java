package com.faforever.client.map.generator;

import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class MapGeneratorService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Getter
  private static final String GENERATED_MAP_NAME = "NeroxisMapGenerator_%s_%d";
  private static final String GENERATOR_DEFAULT_VERSION = "0.1.1";

  @Getter
  private static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";
  @VisibleForTesting
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";
  @Getter
  private static final int GENERATION_TIMEOUT_SECONDS = 20;
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d\\d?\\d?\\.\\d\\d?\\d?\\.\\d\\d?\\d?");
  private static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("NeroxisMapGenerator_(" + VERSION_PATTERN + ")_(-?\\d+)");
  @Getter
  private final Path generatorExecutablePath;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;
  private final TaskService taskService;

  @Getter
  private Path customMapsDirectory;
  private Random seedGenerator;

  @Inject
  public MapGeneratorService(ApplicationContext applicationContext, PreferencesService preferencesService, TaskService taskService) {
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;
    this.taskService = taskService;

    generatorExecutablePath = preferencesService.getFafDataDirectory().resolve(GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    if (!Files.exists(generatorExecutablePath)) {
      try {
        Files.createDirectory(generatorExecutablePath);
      } catch (IOException e) {
        logger.error("Could not create map generator executable directory.", e);
      }
    }

    seedGenerator = new Random();

    customMapsDirectory = this.preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
  }

  @PostConstruct
  public void postConstruct() {
    deleteGeneratedMaps();
  }


  private void deleteGeneratedMaps() {
    logger.info("Deleting leftover generated maps...");

    if (customMapsDirectory != null && customMapsDirectory.toFile().exists()) {
      Arrays.stream(customMapsDirectory.toFile().listFiles())
          .filter(File::isDirectory)
          .filter(f -> GENERATED_MAP_PATTERN.matcher(f.getName()).matches())
          .map(File::toPath)
          .forEach(f -> noCatch(() -> FileUtils.deleteRecursively(f)));
    }
  }


  public CompletableFuture<String> generateMap() {
    return generateMap(GENERATOR_DEFAULT_VERSION, seedGenerator.nextLong());
  }

  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    matcher.find();
    return generateMap(matcher.group(1), Long.parseLong(matcher.group(2)));
  }


  public CompletableFuture<String> generateMap(String version, long seed) {

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, version);
    File generatorExecutableFile = generatorExecutablePath.resolve(generatorExecutableFileName).toFile();

    CompletableFuture<Void> downloadGeneratorFuture;
    if (!generatorExecutableFile.exists()) {
      if (!VERSION_PATTERN.matcher(version).matches()) {
        logger.error("Unsupported generator version: {}", version);
        return CompletableFuture.supplyAsync(() -> {
          throw new RuntimeException("Unsupported generator version: " + version);
        });
      }

      logger.info("Downloading MapGenerator version: {}", version);
      DownloadMapGeneratorTask downloadMapGeneratorTask = applicationContext.getBean(DownloadMapGeneratorTask.class);
      downloadMapGeneratorTask.setVersion(version);
      downloadGeneratorFuture = taskService.submitTask(downloadMapGeneratorTask).getFuture();
    } else {
      logger.info("Found MapGenerator version: {}", version);
      downloadGeneratorFuture = CompletableFuture.completedFuture(null);
    }

    String mapFilename = String.format(MapGeneratorService.getGENERATED_MAP_NAME(), version, seed);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(version);
    generateMapTask.setSeed(seed);
    generateMapTask.setGeneratorExecutableFile(generatorExecutableFile);
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
