package com.faforever.client.map.generator;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubRelease;
import com.faforever.client.util.Assert;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class MapGeneratorService implements DisposableBean {

  /**
   * Naming template for generated maps. It is all lower case because server expects lower case names for maps.
   */
  public static final String GENERATED_MAP_NAME = "neroxis_map_generator_%s_%s";
  public static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";
  @VisibleForTesting
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";
  public static final int GENERATION_TIMEOUT_SECONDS = 60 * 3;
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d\\d?\\d?\\.\\d\\d?\\d?\\.\\d\\d?\\d?");
  protected static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("neroxis_map_generator_(" + VERSION_PATTERN + ")_(.*)");

  private final TaskService taskService;
  private final ClientProperties clientProperties;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;
  private final WebClient defaultWebClient;
  private final ObjectFactory<GenerateMapTask> generateMapTaskFactory;
  private final ObjectFactory<DownloadMapGeneratorTask> downloadMapGeneratorTaskFactory;
  private final ObjectFactory<GeneratorOptionsTask> generatorOptionsTaskFactory;

  private ComparableVersion defaultGeneratorVersion;

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
  private Mono<ComparableVersion> queryMaxSupportedVersion() {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMaxSupportedMajorVersion() + 1));

    return defaultWebClient.get()
        .uri(clientProperties.getMapGenerator().getQueryVersionsUrl())
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToFlux(GitHubRelease.class)
        .map(release -> new ComparableVersion(release.getTagName()))
        .filter(version -> version.compareTo(maxVersion) < 0 && minVersion.compareTo(version) < 0)
        .sort(Comparator.naturalOrder())
                           .last()
                           .switchIfEmpty(Mono.error(new RuntimeException("No valid generator version found")));
  }

  public Mono<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    if (!matcher.find()) {
      return Mono.error(new InvalidParameterException("Map name is not a generated map"));
    }

    ComparableVersion version = new ComparableVersion(matcher.group(1));
    String seed = matcher.group(2);

    Path generatorExecutablePath = getGeneratorExecutablePath(version);

    Mono<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(version);

    GenerateMapTask generateMapTask = generateMapTaskFactory.getObject();
    generateMapTask.setVersion(version);
    generateMapTask.setMapName(mapName);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);

    return downloadGeneratorFuture.then(Mono.defer(() -> taskService.submitTask(generateMapTask).getMono()));
  }

  public Mono<String> generateMap(GeneratorOptions generatorOptions) {
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);

    Mono<Void> downloadGeneratorFuture = downloadGeneratorIfNecessary(defaultGeneratorVersion);

    GenerateMapTask generateMapTask = generateMapTaskFactory.getObject();
    generateMapTask.setVersion(defaultGeneratorVersion);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);
    generateMapTask.setGeneratorOptions(generatorOptions);

    return downloadGeneratorFuture.then(Mono.defer(() -> taskService.submitTask(generateMapTask).getMono()));
  }

  public Mono<Void> downloadGeneratorIfNecessary(ComparableVersion version) {
    ComparableVersion minVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(String.valueOf(clientProperties.getMapGenerator()
        .getMaxSupportedMajorVersion() + 1));
    if (version.compareTo(maxVersion) >= 0) {
      return Mono.error(new UnsupportedVersionException("New version not supported"));
    }
    if (version.compareTo(minVersion) < 0) {
      return Mono.error(new OutdatedVersionException("Old Version not supported"));
    }
    Path generatorExecutablePath = getGeneratorExecutablePath(version);

    if (!Files.exists(generatorExecutablePath)) {
      if (!VERSION_PATTERN.matcher(version.toString()).matches()) {
        log.warn("Unsupported generator version: {}", version);
        return Mono.error(new UnsupportedVersionException("Unsupported generator version: " + version));
      }

      log.info("Downloading MapGenerator version: {}", version);
      DownloadMapGeneratorTask downloadMapGeneratorTask = downloadMapGeneratorTaskFactory.getObject();
      downloadMapGeneratorTask.setVersion(version);
      return taskService.submitTask(downloadMapGeneratorTask).getMono();
    } else {
      log.info("Found MapGenerator version: {}", version);
      return Mono.empty();
    }
  }

  @Cacheable(value = CacheNames.MAP_GENERATOR, sync = true)
  public Mono<Void> getNewestGenerator() {
    return queryMaxSupportedVersion().doOnNext(newVersion -> defaultGeneratorVersion = newVersion)
                                     .flatMap(this::downloadGeneratorIfNecessary);
  }

  public Mono<List<String>> getGeneratorSymmetries() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--symmetries");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  public Mono<List<String>> getGeneratorStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  public Mono<List<String>> getGeneratorTerrainStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--terrain-styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  public Mono<List<String>> getGeneratorTextureStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--texture-styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  public Mono<List<String>> getGeneratorResourceStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--resource-styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  public Mono<List<String>> getGeneratorPropStyles() {
    Assert.checkNullIllegalState(defaultGeneratorVersion, "Generator version not set");
    GeneratorOptionsTask generatorOptionsTask = generatorOptionsTaskFactory.getObject();
    Path generatorExecutablePath = getGeneratorExecutablePath(defaultGeneratorVersion);
    generatorOptionsTask.setVersion(defaultGeneratorVersion);
    generatorOptionsTask.setQuery("--prop-styles");
    generatorOptionsTask.setGeneratorExecutableFile(generatorExecutablePath);
    return taskService.submitTask(generatorOptionsTask).getMono();
  }

  @NotNull
  public Path getGeneratorExecutablePath(ComparableVersion defaultGeneratorVersion) {
    return dataPrefs.getMapGeneratorDirectory()
        .resolve(String.format(GENERATOR_EXECUTABLE_FILENAME, defaultGeneratorVersion));
  }

  public boolean isGeneratedMap(String mapName) {
    return GENERATED_MAP_PATTERN.matcher(mapName).matches();
  }

  /**
   * Generates multiple maps with different seeds and returns their results.
   * Each map generation is independent - if one fails, others will continue.
   * Progress is tracked and emitted after each successful generation.
   *
   * @param baseOptions the base options for map generation (same for all maps)
   * @param mapCount the number of maps to generate
   * @param seeds the list of seeds for each map
   * @return Mono emitting a list of MapGenerationResult objects (including failed ones with isSuccess=false)
   */
  public Mono<List<MapGenerationResult>> generateMultipleMaps(
      GeneratorOptions baseOptions, 
      int mapCount, 
      List<Long> seeds
  ) {
    if (baseOptions == null) {
      return Mono.error(new IllegalStateException("baseOptions must not be null"));
    }
    if (seeds == null) {
      return Mono.error(new IllegalStateException("seeds must not be null"));
    }
    
    if (mapCount <= 0) {
      return Mono.error(new IllegalArgumentException("mapCount must be positive"));
    }
    if (seeds.size() != mapCount) {
      return Mono.error(new IllegalArgumentException("seeds size must match mapCount"));
    }

    log.info("Starting multiple map generation: {} maps with {} seeds", mapCount, seeds.size());

    return downloadGeneratorIfNecessary(defaultGeneratorVersion)
        .then(Mono.defer(() -> Flux.fromIterable(seeds)
                                 .concatMap(seed -> {
              GeneratorOptions optionsWithSeed = createOptionsWithSeed(baseOptions, seed);
              int currentIndex = seeds.indexOf(seed) + 1;
              return generateSingleMapWithErrorHandling(defaultGeneratorVersion, optionsWithSeed, seed, currentIndex, mapCount);
            })
                                 .collect(Collectors.toList())));
  }

  /**
   * Generates multiple maps in parallel using flatMap.
   * Each map generation runs concurrently via TaskService, significantly speeding up bulk generation.
   * Uses availableProcessors as maxConcurrency for balanced resource usage.
   * 
   * @param baseOptions the base options for map generation (same for all maps)
   * @param mapCount the number of maps to generate
   * @param seeds the list of seeds for each map
   * @return Mono emitting a list of MapGenerationResult objects
   */
  public Mono<List<MapGenerationResult>> generateMultipleMapsParallel(
      GeneratorOptions baseOptions, 
      int mapCount, 
      List<Long> seeds
  ) {
    if (baseOptions == null) {
      return Mono.error(new IllegalStateException("baseOptions must not be null"));
    }
    if (seeds == null) {
      return Mono.error(new IllegalStateException("seeds must not be null"));
    }
    
    if (mapCount <= 0) {
      return Mono.error(new IllegalArgumentException("mapCount must be positive"));
    }
    if (seeds.size() != mapCount) {
      return Mono.error(new IllegalArgumentException("seeds size must match mapCount"));
    }

    log.info("Starting parallel map generation: {} maps with {} seeds", mapCount, seeds.size());

    return downloadGeneratorIfNecessary(defaultGeneratorVersion)
        .then(Mono.defer(() -> 
            Flux.fromIterable(seeds)
                .flatMap(seed -> {
                  GeneratorOptions optionsWithSeed = createOptionsWithSeed(baseOptions, seed);
                  int currentIndex = seeds.indexOf(seed) + 1;
                  return generateSingleMapWithErrorHandling(defaultGeneratorVersion, optionsWithSeed, seed, currentIndex, mapCount);
                }, Math.min(mapCount, Runtime.getRuntime().availableProcessors())) // maxConcurrency limited by CPU cores
                .collect(Collectors.toList())
        ));
  }

  /**
   * Generates a single map with error handling. This method is used for multiple map generation
   * to allow the controller to track progress.
   *
   * @param version the generator version
   * @param generatorOptions the options for generation
   * @param seed the seed for this generation (for logging)
   * @param currentMapIndex the current map index (1-based)
   * @param totalMaps the total number of maps being generated
   * @return Mono emitting MapGenerationResult with isSuccess flag and errorMessage if failed
   */
  public Mono<MapGenerationResult> generateSingleMapWithErrorHandling(
      ComparableVersion version, 
      GeneratorOptions generatorOptions, 
      Long seed,
      int currentMapIndex,
      int totalMaps
  ) {
    return generateSingleMap(version, generatorOptions, currentMapIndex, totalMaps)
        .timeout(java.time.Duration.ofSeconds(GENERATION_TIMEOUT_SECONDS))
        .map(mapName -> {
          Path mapDirectory = forgedAlliancePrefs.getMapsDirectory().resolve(mapName);
          return new MapGenerationResult(
              mapName,
              generatorOptions,
              mapDirectory,
              false,
              Optional.empty(),
              true
          );
        })
        .onErrorMap(exception -> {
          log.error("Map generation failed for seed {}, {}, {}", seed, generatorOptions, version, exception);
          String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
          return new RuntimeException(errorMessage, exception);
        });
  }

  /**
   * Creates a GeneratorOptions with a specific seed from base options.
   *
   * @param baseOptions the base options to copy from
   * @param seed the seed to use
   * @return a new GeneratorOptions with the specified seed
   */
  private GeneratorOptions createOptionsWithSeed(GeneratorOptions baseOptions, Long seed) {
    return GeneratorOptions.builder()
        .spawnCount(baseOptions.spawnCount())
        .numTeams(baseOptions.numTeams())
        .mapSize(baseOptions.mapSize())
        .seed(seed.toString())
        .generationType(baseOptions.generationType())
        .symmetry(baseOptions.symmetry())
        .style(baseOptions.style())
        .terrainStyle(baseOptions.terrainStyle())
        .textureStyle(baseOptions.textureStyle())
        .resourceStyle(baseOptions.resourceStyle())
        .propStyle(baseOptions.propStyle())
        .reclaimDensity(baseOptions.reclaimDensity())
        .resourceDensity(baseOptions.resourceDensity())
        .commandLineArgs(baseOptions.commandLineArgs())
        .build();
  }

  /**
   * Generates a single map and returns its name.
   *
   * @param version the generator version
   * @param generatorOptions the options for generation
   * @param currentMapIndex the current map index (1-based)
   * @param totalMaps the total number of maps being generated
   * @return Mono emitting the map name
   */
  private Mono<String> generateSingleMap(
      ComparableVersion version, 
      GeneratorOptions generatorOptions,
      int currentMapIndex,
      int totalMaps
  ) {
    Path generatorExecutablePath = getGeneratorExecutablePath(version);

    GenerateMapTask generateMapTask = generateMapTaskFactory.getObject();
    generateMapTask.setVersion(version);
    generateMapTask.setGeneratorExecutableFile(generatorExecutablePath);
    generateMapTask.setGeneratorOptions(generatorOptions);
    generateMapTask.setCurrentMapIndex(currentMapIndex);
    generateMapTask.setTotalMaps(totalMaps);

    return taskService.submitTask(generateMapTask).getMono();
  }

}
