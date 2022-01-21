package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
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
@Setter
public class GenerateMapTask extends CompletableTask<String> {
  private static final Logger generatorLogger = LoggerFactory.getLogger("faf-map-generator");

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  private Path generatorExecutableFile;
  private ComparableVersion version;
  private GeneratorOptions generatorOptions;
  private String seed;
  private String mapName;

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

    GeneratorCommand.GeneratorCommandBuilder generatorCommandBuilder = GeneratorCommand.builder()
        .version(version)
        .seed(seed)
        .generatorExecutableFile(generatorExecutableFile)
        .mapName(mapName);

    if (generatorOptions != null) {
      generatorCommandBuilder.spawnCount(generatorOptions.getSpawnCount())
          .numTeams(generatorOptions.getNumTeams())
          .mapSize(generatorOptions.getMapSize())
          .generationType(generatorOptions.getGenerationType())
          .landDensity(generatorOptions.getLandDensity())
          .plateauDensity(generatorOptions.getPlateauDensity())
          .mountainDensity(generatorOptions.getMountainDensity())
          .rampDensity(generatorOptions.getRampDensity())
          .mexDensity(generatorOptions.getMexDensity())
          .reclaimDensity(generatorOptions.getReclaimDensity())
          .style(generatorOptions.getStyle())
          .biome(generatorOptions.getBiome())
          .commandLineArgs(generatorOptions.getCommandLineArgs());
    }

    Path workingDirectory = preferencesService.getPreferences().getForgedAlliance().getMapsDirectory();

    try {
      List<String> command = generatorCommandBuilder.build().getCommand();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.directory(workingDirectory.toFile());
      processBuilder.command(command);

      log.info("Starting map generator in directory: {} with command: {}",
          processBuilder.directory(), String.join(" ", processBuilder.command()));

      Process process = processBuilder.start();
      OsUtils.gobbleLines(process.getInputStream(), msg -> {
        generatorLogger.info(msg);
        if (mapName == null || mapName.isBlank()) {
          Matcher mapNameMatcher = MapGeneratorService.GENERATED_MAP_PATTERN.matcher(msg);
          if (mapNameMatcher.find()) {
            mapName = mapNameMatcher.group();
          }
        }
      });
      OsUtils.gobbleLines(process.getErrorStream(), generatorLogger::error);
      process.waitFor(MapGeneratorService.GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (process.isAlive() && !generatorOptions.getCommandLineArgs().contains("--visualize")) {
        log.warn("Map generation timed out, killing process...");
        process.destroyForcibly();
        notificationService.addImmediateErrorNotification(new RuntimeException("Map generation timed out"), "game.mapGeneration.failed.message");
      } else if (!process.isAlive()) {
        eventBus.post(new MapGeneratedEvent(mapName));
      }
    } catch (Exception e) {
      log.error("Could not start map generator.", e);
      throw new RuntimeException(e);
    }

    return mapName;
  }
}
