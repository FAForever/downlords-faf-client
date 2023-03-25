package com.faforever.client.map.generator;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubRelease;
import com.faforever.client.util.Assert;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Lazy
@Service
@Slf4j
public class MapGeneratorService implements DisposableBean {

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

  private final TaskService taskService;
  private final ClientProperties clientProperties;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;
  private final WebClient webClient;
  private final ObjectFactory<GenerateMapTask> generateMapTaskFactory;
  private final ObjectFactory<DownloadMapGeneratorTask> downloadMapGeneratorTaskFactory;
  private final ObjectFactory<GeneratorOptionsTask> generatorOptionsTaskFactory;

  private ComparableVersion defaultGeneratorVersion;

  public MapGeneratorService(TaskService taskService, ClientProperties clientProperties, ForgedAlliancePrefs forgedAlliancePrefs, DataPrefs dataPrefs, Builder webClientBuilder,
                             ObjectFactory<GenerateMapTask> generateMapTaskFactory,
                             ObjectFactory<DownloadMapGeneratorTask> downloadMapGeneratorTaskFactory,
                             ObjectFactory<GeneratorOptionsTask> generatorOptionsTaskFactory) {
    this.taskService = taskService;
    this.clientProperties = clientProperties;
    this.forgedAlliancePrefs = forgedAlliancePrefs;
    this.dataPrefs = dataPrefs;
    this.webClient = webClientBuilder.build();
    this.generateMapTaskFactory = generateMapTaskFactory;
    this.downloadMapGeneratorTaskFactory = downloadMapGeneratorTaskFactory;
    this.generatorOptionsTaskFactory = generatorOptionsTaskFactory;
  }

  @Override
  public void destroy() throws Exception {
    deleteGeneratedMaps();
  }

  private void deleteGeneratedMaps() {
    log.info("Deleting generated maps");
    Path customMapsDirectory = forgedAlliancePrefs.getMapsDirectory();
    if (customMapsDirectory != null && customMapsDirectory.toFile().exists()) {
      try (Stream<Path> listOfMapFiles = Files.list(customMapsDirectory)) {
        listOfMapFiles.filter(Files::isDirectory)
            .filter(mapPath -> GENERATED_MAP_PATTERN.matcher(mapPath.getFileName().toString()).matches())
            .forEach(generatedMapPath -> {
              try {
                FileSystemUtils.deleteRecursively(generatedMapPath);
              } catch (IOException e) {
                log.warn("Could not delete generated map directory {}", generatedMapPath, e);
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
  private CompletableFuture<ComparableVersion> queryMaxSupportedVersion() {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMaxSupportedMajorVersion() + 1));

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
        .toFuture();
  }

  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    if (!matcher.find()) {
      return CompletableFuture.failedFuture(new InvalidParameterException("Map name is not a generated map"));
    }

    ComparableVersion version = new ComparableVersion(matcher.group(1));
    String seed = matcher.group(2);

    Path generatorExecutablePath = getGeneratorExecutablePath(version);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(version);

    GenerateMapTask generateMapTask = generateMapTaskFactory.getObject();
    generateMapTask.setVersion(version);
    generateMapTask.setSeed(seed);
    generateMapTask.setMapName(mapName);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<String> generateMap(GeneratorOptions generatorOptions) {
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);

    CompletableFuture<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(defaultGeneratorVersion);

    GenerateMapTask generateMapTask = generateMapTaskFactory.getObject();
    generateMapTask.setVersion(defaultGeneratorVersion);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);
    generateMapTask.setGeneratorOptions(generatorOptions);

    return downloadGeneratorFuture.thenCompose((aVoid) -> taskService.submitTask(generateMapTask).getFuture());
  }

  public CompletableFuture<Void> downloadGeneratorIfNecessary(ComparableVersion version) {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMaxSupportedMajorVersion() + 1));
    if (version.compareTo(maxVersion) >= 0) {
      return CompletableFuture.failedFuture(new UnsupportedVersionException("New version not supported"));
    }
    if (version.compareTo(minVersion) < 0) {
      return CompletableFuture.failedFuture(new OutdatedVersionException("Old Version not supported"));
    }
    Path generatorExecutablePath = getGeneratorExecutablePath(version);

    if (!Files.exists(generatorExecutablePath)) {
      if (!VERSION_PATTERN.matcher(version.toString()).matches()) {
        log.warn("Unsupported generator version: {}", version);
        return CompletableFuture.failedFuture(new UnsupportedVersionException("Unsupported generator version: " + version));
      }

      log.info("Downloading MapGenerator version: {}", version);
      DownloadMapGeneratorTask downloadMapGeneratorTask = downloadMapGeneratorTaskFactory.getObject();
      downloadMapGeneratorTask.setVersion(version);
      return taskService.submitTask(downloadMapGeneratorTask).getFuture();
    } else {
      log.info("Found MapGenerator version: {}", version);
      return CompletableFuture.completedFuture(null);
    }
  }

  @Cacheable(value = CacheNames.MAP_GENERATOR, sync = true)
  public CompletableFuture<Void> getNewestGenerator() {
    return queryMaxSupportedVersion().thenAccept(newVersion -> defaultGeneratorVersion = newVersion)
        .thenCompose(aVoid -> downloadGeneratorIfNecessary(defaultGeneratorVersion));
  }

  public CompletableFuture<List<String>> getGeneratorStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getFuture();
  }

  @NotNull
  public Path getGeneratorExecutablePath(ComparableVersion defaultGeneratorVersion) {
    return dataPrefs.getMapGeneratorDirectory()
        .resolve(String.format(GENERATOR_EXECUTABLE_FILENAME, defaultGeneratorVersion));
  }

  public boolean isGeneratedMap(String mapName) {
    return GENERATED_MAP_PATTERN.matcher(mapName).matches();
  }
}
