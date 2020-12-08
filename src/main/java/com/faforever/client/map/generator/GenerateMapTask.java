package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.os.OsUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.google.common.eventbus.EventBus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GenerateMapTask extends CompletableTask<String> {
  private static final Logger generatorLogger = LoggerFactory.getLogger("faf-map-generator");

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Setter
  private ComparableVersion version;
  @Setter
  private Path generatorExecutableFile;
  @Setter
  private String mapFilename;
  @Setter
  private Integer spawnCount;
  @Setter
  private Integer mapSize;
  @Setter
  private String seed;
  @Setter
  private Float landDensity;
  @Setter
  private Float plateauDensity;
  @Setter
  private Float mountainDensity;
  @Setter
  private Float rampDensity;
  @Setter
  private GenerationType generationType;

  @Inject
  public GenerateMapTask(PreferencesService preferencesService, NotificationService notificationService, I18n i18n, EventBus eventBus) {
    super(Priority.HIGH);

    this.preferencesService = preferencesService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @Override
  protected String call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");

    updateTitle(i18n.get("game.mapGeneration.generateMap.title", version));

    GeneratorCommandBuilder generatorCommandBuilder = GeneratorCommandBuilder.create()
        .version(version)
        .spawnCount(spawnCount)
        .mapSize(mapSize)
        .seed(seed)
        .generatorExecutableFilePath(generatorExecutableFile)
        .generationType(generationType)
        .landDensity(landDensity)
        .plateauDensity(plateauDensity)
        .mountainDensity(mountainDensity)
        .rampDensity(rampDensity)
        .mapFilename(mapFilename);

    Path workingDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();

    try {
      List<String> command = generatorCommandBuilder.build();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.directory(workingDirectory.toFile());
      processBuilder.command(command);
      processBuilder.environment().put("LOG_DIR", preferencesService.getFafLogDirectory().toAbsolutePath().toString());

      log.info("Starting map generator in directory: {} with command: {}",
          processBuilder.directory(), processBuilder.command().stream().reduce((l, r) -> l + " " + r).get());

      Process process = processBuilder.start();
      OsUtils.gobbleLines(process.getInputStream(), msg -> {
        generatorLogger.info(msg);
        if (mapFilename == null || mapFilename.isBlank()) {
          Matcher mapNameMatcher = MapGeneratorService.GENERATED_MAP_PATTERN.matcher(msg);
          if (mapNameMatcher.find()) {
            mapFilename = mapNameMatcher.group();
          }
        }
      });
      OsUtils.gobbleLines(process.getErrorStream(), generatorLogger::error);
      process.waitFor(MapGeneratorService.GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (process.isAlive()) {
        log.warn("Map generation timed out, killing process...");
        process.destroyForcibly();
        notificationService.addNotification(new ImmediateNotification(i18n.get("game.mapGeneration.failed.title"),
            i18n.get("game.mapGeneration.failed.message"), Severity.ERROR));
      } else {
        eventBus.post(new MapGeneratedEvent(mapFilename));
      }
    } catch (Exception e) {
      log.error("Could not start map generator.", e);
      throw new RuntimeException(e);
    }

    return mapFilename;
  }
}
