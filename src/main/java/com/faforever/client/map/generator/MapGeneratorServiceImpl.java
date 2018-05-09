package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FileUtils;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class MapGeneratorServiceImpl implements MapGeneratorService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GENERATED_MAP_NAME = "NeroxisMapGenerator_%d_%s";
  private static final String GENERATOR_DEFAULT_VERSION = "0.1.0";
  private static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";

  private static final Set<String> AVAILABLE_VERSIONS = new HashSet<>(Arrays.asList(
      "0.0.1-prealpha",
      "0.1.0"
  ));

  private static final int GENERATION_TIMEOUT_SECONDS = 20;

  private static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("NeroxisMapGenerator_(-?\\d+)_(\\S+)");

  private final PreferencesService preferencesService;
  private final TaskService taskService;
  private final I18n i18n;
  private final NotificationService notificationService;
  private final EventBus eventBus;

  private Path customMapsDirectory;
  private Random seedGenerator;

  @Inject
  public MapGeneratorServiceImpl(PreferencesService preferencesService, TaskService taskService, I18n i18n, NotificationService notificationService, EventBus eventBus) {
    this.preferencesService = preferencesService;
    this.taskService = taskService;
    this.i18n = i18n;
    this.notificationService = notificationService;
    this.eventBus = eventBus;


    seedGenerator = new Random();

    customMapsDirectory = this.preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    deleteGeneratedMaps();
  }

  @PostConstruct
  void postConstruct() {

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
    return generateMap(seedGenerator.nextLong(), GENERATOR_DEFAULT_VERSION);
  }

  //TODO: PROTECT AGAINST mapName INJECTION!!!!!
  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    matcher.find();
    return generateMap(Long.parseLong(matcher.group(1)), matcher.group(2));
  }

  public CompletableFuture<String> generateMap(long seed, String version) {
    if (!AVAILABLE_VERSIONS.contains(version)) {
      throw new RuntimeException("Unsupported generator version!");
    }

    String nativeDir = System.getProperty("nativeDir", "lib");
    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, version);
    File generatorExecutableFile = new File(nativeDir + "/" + generatorExecutableFileName);

    notificationService.addNotification(new TransientNotification(i18n.get("game.mapGeneration.notification.title"), i18n.get("game.mapGeneration.notification.message", version, seed)));

    CompletableFuture<String> downloadGeneratorFuture = CompletableFuture.supplyAsync(() -> {
      if (!generatorExecutableFile.exists()) {
        logger.info("Downloading MapGenerator version: {}", version);
        //TODO: download older mapgenerator version
      } else {
        logger.info("Found MapGenerator version: {}", version);
      }

      return null;
    });


    return downloadGeneratorFuture.thenApply((aVoid) -> {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.inheritIO();
      processBuilder.directory(customMapsDirectory.toFile());
      String mapFolder = String.format(GENERATED_MAP_NAME, seed, version);
      processBuilder.command("java", "-jar", generatorExecutableFile.getAbsolutePath(), mapFolder, seed + "", version);

      logger.info("Starting map generator in directory: {} with command: {}", processBuilder.directory(), processBuilder.command().stream().reduce((l, r) -> l + " " + r).get());
      try {
        Process process = processBuilder.start();
        process.waitFor(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (process.isAlive()) {
          process.destroyForcibly();
          notificationService.addNotification(new ImmediateNotification("Map generation failed.", "The requested map could not be generated, the map generation process did not terminate within timeout. Killing process.", Severity.ERROR));
        } else {
          eventBus.post(new MapGeneratedEvent(mapFolder));
        }

        return mapFolder;
      } catch (IOException | InterruptedException e) {
        logger.error("Could not start map generator.", e);
        throw new RuntimeException();
      }
    });
  }

  public boolean isGeneratedMap(String mapName) {
    return GENERATED_MAP_PATTERN.matcher(mapName).matches();
  }
}
