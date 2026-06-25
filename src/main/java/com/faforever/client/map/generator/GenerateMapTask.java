package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsUtils;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.CompletableTask;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

  private final NotificationService notificationService;
  private final I18n i18n;
  private final OperatingSystem operatingSystem;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private Path generatorExecutableFile;
  private ComparableVersion version;
  private GeneratorOptions generatorOptions;
  private String mapName;

  @Autowired
  public GenerateMapTask(NotificationService notificationService, I18n i18n, OperatingSystem operatingSystem,
                         ForgedAlliancePrefs forgedAlliancePrefs) {
    super(Priority.HIGH);
    this.forgedAlliancePrefs = forgedAlliancePrefs;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.operatingSystem = operatingSystem;
  }

  @Override
  protected String call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");

    updateTitle(i18n.get("game.mapGeneration.generateMap.title", version));

    GeneratorCommand.GeneratorCommandBuilder generatorCommandBuilder = GeneratorCommand.builder()
                                                                                       .version(version)
                                                                                       .generatorExecutableFile(
                                                                                           generatorExecutableFile)
                                                                                       .javaExecutable(
                                                                                           operatingSystem.getJavaExecutablePath())
                                                                                       .mapName(mapName);

    if (generatorOptions != null) {
      generatorCommandBuilder.spawnCount(generatorOptions.spawnCount())
                             .numTeams(generatorOptions.numTeams())
                             .mapSize(generatorOptions.mapSize())
                             .seed(generatorOptions.seed())
                             .generationType(generatorOptions.generationType())
                             .symmetry(generatorOptions.symmetry())
                             .style(generatorOptions.style())
                             .terrainStyle(generatorOptions.terrainStyle())
                             .textureStyle(generatorOptions.textureStyle())
                             .resourceStyle(generatorOptions.resourceStyle())
                             .propStyle(generatorOptions.propStyle())
                             .resourceDensity(generatorOptions.resourceDensity())
                             .reclaimDensity(generatorOptions.reclaimDensity())
                             .commandLineArgs(generatorOptions.commandLineArgs());
    }

    Path workingDirectory = forgedAlliancePrefs.getMapsDirectory();

    try {
      List<String> command = generatorCommandBuilder.build().getCommand();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.directory(workingDirectory.toFile());
      processBuilder.command(command);

      log.info("Starting map generator in directory: `{}` with command: `{}`", processBuilder.directory(),
               String.join(" ", processBuilder.command()));

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
      if (process.isAlive() && generatorOptions.commandLineArgs() != null && !generatorOptions.commandLineArgs()
                                                                                              .contains(
                                                                                                  "--visualize")) {
        log.warn("Map generation timed out, killing process");
        process.destroyForcibly();
        notificationService.addImmediateErrorNotification(new RuntimeException("Map generation timed out"),
                                                          "game.mapGeneration.failed.message");
      }
    } catch (Exception e) {
      log.error("Could not start map generator", e);
      throw new RuntimeException(e);
    }

    return mapName;
  }
}
