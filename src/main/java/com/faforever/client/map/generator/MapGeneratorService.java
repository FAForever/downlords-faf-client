package com.faforever.client.map.generator;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.io.FileUtils;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
@Slf4j
public class MapGeneratorService implements InitializingBean {

  /**
   * Naming template for generated maps. It is all lower case because server expects lower case names for maps.
   */
  public static final String GENERATED_MAP_NAME = "neroxis_map_generator_%s_%s";
  public static final String GENERATOR_EXECUTABLE_FILENAME = "MapGenerator_%s.jar";
  public static final byte DEFAULT_SPAWN_COUNT = 6;
  public static final byte DEFAULT_LAND_DENSITY = 26;
  @VisibleForTesting
  public static final String GENERATOR_EXECUTABLE_SUB_DIRECTORY = "map_generator";
  public static final int GENERATION_TIMEOUT_SECONDS = 60;
  private static final Pattern VERSION_PATTERN = Pattern.compile("\\d\\d?\\d?\\.\\d\\d?\\d?\\.\\d\\d?\\d?");
  private static final Pattern GENERATED_MAP_PATTERN = Pattern.compile("neroxis_map_generator_(" + VERSION_PATTERN + ")_(-?.*)");
  @Getter
  private final Path generatorExecutablePath;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final TaskService taskService;
  private final ClientProperties clientProperties;

  @Getter
  private final Path customMapsDirectory;
  private final Random seedGenerator;

  @Getter
  private Image generatedMapPreviewImage;

  public MapGeneratorService(ApplicationContext applicationContext, PreferencesService preferencesService, TaskService taskService, NotificationService notificationService, ClientProperties clientProperties) {
    this.applicationContext = applicationContext;
    this.preferencesService = preferencesService;
    this.notificationService = notificationService;
    this.taskService = taskService;

    generatorExecutablePath = preferencesService.getFafDataDirectory().resolve(GENERATOR_EXECUTABLE_SUB_DIRECTORY);
    this.clientProperties = clientProperties;
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
            .forEach(p -> noCatch(() -> FileUtils.deleteRecursively(p)));
      } catch (IOException e) {
        log.error("Could not list custom maps directory for deleting leftover generated maps.", e);
      }
    }
  }

  public CompletableFuture<String> generateMap() {
    ByteBuffer seedBuffer = ByteBuffer.allocate(8);
    seedBuffer.putLong(seedGenerator.nextLong());
    String seedString = Base64.getEncoder().encodeToString(seedBuffer.array());
    return generateMap(queryMaxSupportedVersion(),seedString);
  }

  public CompletableFuture<String> generateMap(byte[] optionArray) {
    ByteBuffer seedBuffer = ByteBuffer.allocate(8);
    seedBuffer.putLong(seedGenerator.nextLong());
    String seedString = Base64.getEncoder().encodeToString(seedBuffer.array());
    String optionString = Base64.getEncoder().encodeToString(optionArray);
    return generateMap(queryMaxSupportedVersion(),seedString+'_'+optionString);
  }

  @VisibleForTesting
  protected String queryNewestVersion() {
    RestTemplate restTemplate = new RestTemplate();

    LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/vnd.github.v3+json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ResponseEntity<String> response = restTemplate.exchange(clientProperties.getMapGenerator().getQueryLatestVersionUrl(), HttpMethod.GET, entity, String.class);
    JsonElement jsonElement = new JsonParser().parse(response.getBody());
    JsonObject mainObject = jsonElement.getAsJsonObject();

    return mainObject.get("tag_name").getAsString();
  }

  @VisibleForTesting
  public String queryMaxSupportedVersion() {
    String version = queryNewestVersion();
    String majorVersion = version.split("\\.")[0];
    if (Integer.parseInt(majorVersion)>clientProperties.getMapGenerator().getMaxSupportedMajorVersion() ||
        Integer.parseInt(majorVersion)<clientProperties.getMapGenerator().getMinSupportedMajorVersion()) {

      RestTemplate restTemplate = new RestTemplate();

      LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Accept", "application/vnd.github.v3+json");
      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<String> response = restTemplate.exchange(clientProperties.getMapGenerator().getQueryVersionsUrl(), HttpMethod.GET, entity, String.class);
      JsonElement jsonElement = new JsonParser().parse(response.getBody());
      for(JsonElement element: jsonElement.getAsJsonArray()) {
        JsonObject mainObject = element.getAsJsonObject();
        version = mainObject.get("tag_name").getAsString();
        majorVersion = version.split("\\.")[0];
        if (Integer.parseInt(majorVersion)<clientProperties.getMapGenerator().getMaxSupportedMajorVersion() &&
            Integer.parseInt(majorVersion)>clientProperties.getMapGenerator().getMinSupportedMajorVersion()) {
          return version;
        }
      }
      throw new RuntimeException("No Valid Generator Version Found");
    }
    return version;
  }

  public CompletableFuture<String> generateMap(String mapName) {
    Matcher matcher = GENERATED_MAP_PATTERN.matcher(mapName);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Map name is not a generated map");
    }
    return generateMap(matcher.group(1), matcher.group(2));
  }


  public CompletableFuture<String> generateMap(String version, String seedAndOptions) {

    String majorVersion = version.split("\\.")[0];
    if (Integer.parseInt(majorVersion)>clientProperties.getMapGenerator().getMaxSupportedMajorVersion()){
      notificationService.addImmediateErrorNotification(new IllegalArgumentException("Map Version Not Supported"), "mapGenerator.tooNewVersion");
    }
    if (Integer.parseInt(majorVersion)<clientProperties.getMapGenerator().getMinSupportedMajorVersion()) {
      notificationService.addImmediateErrorNotification(new IllegalArgumentException("Map Version Not supported"), "mapGenerator.tooOldVersion");
    }
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

    String[] seedParts = seedAndOptions.split("_");
    String seedString = seedParts[0];

    String mapFilename;
    String seed;
    if (majorVersion.equals("0")){
      seed = Long.toString(Long.parseLong(seedString));
      mapFilename = String.format(GENERATED_MAP_NAME, version, seed);
    } else {
      mapFilename = String.format(GENERATED_MAP_NAME, version, seedAndOptions);
      byte[] seedBytes = Base64.getDecoder().decode(seedString);
      ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
      seed = Long.toString(seedWrapper.getLong());
    }
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
