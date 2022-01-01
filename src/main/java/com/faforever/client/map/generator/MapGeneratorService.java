package com.faforever.client.map.generator;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubRelease;
import com.google.common.annotations.VisibleForTesting;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Lazy
@Service
@Slf4j
public class MapGeneratorService implements InitializingBean {

  /**
   * Naming template for generated maps. It is all lower case because server expects lower case names for maps.
   */
  public static final String GENERATED_MAP_NAME = "neroxis_map_generator_%s_%s";
  public static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";
  @VisibleForTesting
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";
  public static final int GENERATION_TIMEOUT_SECONDS = 60 * 3;
  public static final String GENERATOR_RANDOM_STYLE = "RANDOM";
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d\\d?\\d?\\.\\d\\d?\\d?\\.\\d\\d?\\d?");
  protected static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("neroxis_map_generator_(" + VERSION_PATTERN + ")_(.*)");
  @Getter
  private final Path generatorExecutablePath;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;
  private final ClientProperties clientProperties;
  private final WebClient webClient;

  @Getter
  private final Path customMapsDirectory;

  @Getter
  private Image generatedMapPreviewImage;
  @Getter
  @Setter
  private ComparableVersion generatorVersion;

  public MapGeneratorService(ApplicationContext applicationContext, PreferencesService preferencesService,
                             TaskService taskService, ClientProperties clientProperties, WebClient.Builder webClientBuilder) {
    this.applicationContext = applicationContext;
    this.taskService = taskService;
    webClient = webClientBuilder.build();

    generatorExecutablePath = preferencesService.getFafDataDirectory().resolve(GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    this.clientProperties = clientProperties;
    if (!Files.exists(generatorExecutablePath)) {
      try {
        Files.createDirectory(generatorExecutablePath);
      } catch (IOException e) {
        log.error("Could not create map generator executable directory.", e);
      }
    }

    customMapsDirectory = preferencesService.getPreferences().getForgedAlliance().getMapsDirectory();

    try {
      generatedMapPreviewImage = new Image(new ClassPathResource("/images/generatedMapIcon.png").getURL().toString(), true);
    } catch (IOException | NoClassDefFoundError | ExceptionInInitializerError e) {
      log.error("Could not load generated map preview image.", e);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    deleteGeneratedMaps();
  }

  private void deleteGeneratedMaps() {
    log.info("Deleting leftover generated maps...");
    if (customMapsDirectory != null && customMapsDirectory.toFile().exists()) {
      try (Stream<Path> listOfMapFiles = Files.list(customMapsDirectory)) {
        listOfMapFiles
            .filter(Files::isDirectory)
            .filter(p -> GENERATED_MAP_PATTERN.matcher(p.getFileName().toString()).matches())
            .forEach(p -> {
              try {
                FileUtils.deleteRecursively(p);
              } catch (IOException e) {
                log.warn("Could not delete generated map directory {}", p, e);
              }
            });
      } catch (IOException e) {
        log.error("Could not list custom maps directory for deleting leftover generated maps.", e);
      } catch (RuntimeException e) {
        log.error("Could not delete generated map folder");
      }
    }
  }

  @VisibleForTesting
  @Cacheable(value = CacheNames.MAP_GENERATOR, sync = true)
  public ComparableVersion queryMaxSupportedVersion() {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator().getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator().getMaxSupportedMajorVersion() + 1));

    return webClient.get()
        .uri(clientProperties.getMapGenerator().getQueryVersionsUrl())
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToFlux(GitHubRelease.class)
        .map(release -> new ComparableVersion(release.getTagName()))
        .filter(version -> version.compareTo(maxVersion) < 0 && minVersion.compareTo(version) < 0)
        .sort(Comparator.naturalOrder())
        .last()
        .switchIfEmpty(Mono.error(new RuntimeException("No valid generator version found")))
        .block();
  }

  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    if (!matcher.find()) {
      return CompletableFuture.failedFuture(new InvalidParameterException("Map name is not a generated map"));
    }

    ComparableVersion version = new ComparableVersion(matcher.group(1));
    String seed = matcher.group(2);

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, version);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(version);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(version);
    generateMapTask.setSeed(seed);
    generateMapTask.setMapFilename(mapName);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<String> generateMap(int spawnCount, int mapSize, int numTeams, Map<String, Float> optionMap, GenerationType generationType) {

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, generatorVersion);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(generatorVersion);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(generatorVersion);
    generateMapTask.setSpawnCount(spawnCount);
    generateMapTask.setMapSize(mapSize);
    generateMapTask.setNumTeams(numTeams);
    generateMapTask.setGenerationType(generationType);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);
    if (optionMap.containsKey("landDensity")) {
      generateMapTask.setLandDensity(optionMap.get("landDensity"));
    }
    if (optionMap.containsKey("plateauDensity")) {
      generateMapTask.setPlateauDensity(optionMap.get("plateauDensity"));
    }
    if (optionMap.containsKey("mountainDensity")) {
      generateMapTask.setMountainDensity(optionMap.get("mountainDensity"));
    }
    if (optionMap.containsKey("rampDensity")) {
      generateMapTask.setRampDensity(optionMap.get("rampDensity"));
    }
    if (optionMap.containsKey("mexDensity")) {
      generateMapTask.setMexDensity(optionMap.get("mexDensity"));
    }
    if (optionMap.containsKey("reclaimDensity")) {
      generateMapTask.setReclaimDensity(optionMap.get("reclaimDensity"));
    }

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<String> generateMap(int spawnCount, int mapSize, int numTeams, String style) {

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, generatorVersion);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(generatorVersion);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(generatorVersion);
    generateMapTask.setSpawnCount(spawnCount);
    generateMapTask.setMapSize(mapSize);
    generateMapTask.setNumTeams(numTeams);
    generateMapTask.setStyle(style);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<String> generateMapWithArgs(String commandLineArgs) {

    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, generatorVersion);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(generatorVersion);

    GenerateMapTask generateMapTask = applicationContext.getBean(GenerateMapTask.class);
    generateMapTask.setVersion(generatorVersion);
    generateMapTask.setCommandLineArgs(commandLineArgs);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<Void> downloadGeneratorIfNecessary(ComparableVersion version) {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator().getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator().getMaxSupportedMajorVersion() + 1));
    if (version.compareTo(maxVersion) >= 0) {
      return CompletableFuture.failedFuture(new UnsupportedVersionException("New version not supported"));
    }
    if (version.compareTo(minVersion) < 0) {
      return CompletableFuture.failedFuture(new OutdatedVersionException("Old Version not supported"));
    }
    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, version);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    if (!Files.exists(generatorExecutablePath)) {
      if (!VERSION_PATTERN.matcher(version.toString()).matches()) {
        log.warn("Unsupported generator version: {}", version);
        return CompletableFuture.failedFuture(new UnsupportedVersionException("Unsupported generator version: " + version));
      }

      log.info("Downloading MapGenerator version: {}", version);
      DownloadMapGeneratorTask downloadMapGeneratorTask = applicationContext.getBean(DownloadMapGeneratorTask.class);
      downloadMapGeneratorTask.setVersion(version.toString());
      return taskService.submitTask(downloadMapGeneratorTask).getFuture();
    } else {
      log.info("Found MapGenerator version: {}", version);
      return CompletableFuture.completedFuture(null);
    }
  }

  public CompletableFuture<List<String>> getGeneratorStyles() {
    String generatorExecutableFileName = String.format(GENERATOR_EXECUTABLE_FILENAME, generatorVersion);
    Path generatorExecutablePath = this.generatorExecutablePath.resolve(generatorExecutableFileName);

    CompletableFuture<Void> downloadTask;

    if (!Files.exists(generatorExecutablePath)) {
      if (!VERSION_PATTERN.matcher(generatorVersion.toString()).matches()) {
        log.warn("Unsupported generator version: {}", generatorVersion);
        return CompletableFuture.failedFuture(new UnsupportedVersionException("Unsupported generator version: " + generatorVersion));
      }

      log.info("Downloading MapGenerator version: {}", generatorVersion);
      DownloadMapGeneratorTask downloadMapGeneratorTask = applicationContext.getBean(DownloadMapGeneratorTask.class);
      downloadMapGeneratorTask.setVersion(generatorVersion.toString());
      downloadTask = taskService.submitTask(downloadMapGeneratorTask).getFuture();
    } else {
      downloadTask = CompletableFuture.completedFuture(null);
    }
    return downloadTask.thenCompose(aVoid -> {
      GeneratorOptionsTask generatorOptionsTask = applicationContext.getBean(GeneratorOptionsTask.class);
      generatorOptionsTask.setVersion(generatorVersion);
      generatorOptionsTask.setQuery("--styles");
      generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
      return taskService.submitTask(generatorOptionsTask).getFuture();
    });
  }

  public boolean isGeneratedMap(String mapName) {
    return GENERATED_MAP_PATTERN.matcher(mapName).matches();
  }
}
