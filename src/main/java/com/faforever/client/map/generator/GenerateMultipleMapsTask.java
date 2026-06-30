package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsUtils;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.CompletableTask;
import lombok.Getter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Setter
@Getter
public class GenerateMultipleMapsTask extends CompletableTask<List<String>> {
  private static final Logger generatorLogger = LoggerFactory.getLogger("faf-map-generator");

  private final NotificationService notificationService;
  private final I18n i18n;
  private final OperatingSystem operatingSystem;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private Path generatorExecutableFile;
  private ComparableVersion version;
  private GeneratorOptions baseOptions;
  private Long seed;
  private int mapCount;

  @Autowired
  public GenerateMultipleMapsTask(NotificationService notificationService, I18n i18n, OperatingSystem operatingSystem,
                                  ForgedAlliancePrefs forgedAlliancePrefs) {
    super(Priority.HIGH);
    this.forgedAlliancePrefs = forgedAlliancePrefs;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.operatingSystem = operatingSystem;
  }

  @Override
  protected List<String> call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");
    Objects.requireNonNull(baseOptions, "Base options haven't been set.");

    updateTitle(i18n.get("game.mapGeneration.generateMap.title", version));

    GeneratorOptions options = GeneratorOptions.builder()
                                               .spawnCount(baseOptions.spawnCount())
                                               .numTeams(baseOptions.numTeams())
                                               .mapSize(baseOptions.mapSize())
                                               .seed(Optional.ofNullable(seed).map(Object::toString).orElse(null))
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

    Path workingDirectory = forgedAlliancePrefs.getMapsDirectory();

    GeneratorCommand.GeneratorCommandBuilder commandBuilder = GeneratorCommand.builder()
                                                                              .version(version)
                                                                              .generatorExecutableFile(
                                                                                  generatorExecutableFile)
                                                                              .javaExecutable(
                                                                                  operatingSystem.getJavaExecutablePath())
                                                                              .numToGenerate(mapCount)
                                                                              .generatorOptions(options)
                                                                              .spawnCount(options.spawnCount())
                                                                              .numTeams(options.numTeams())
                                                                              .mapSize(options.mapSize())
                                                                              .seed(options.seed())
                                                                              .generationType(options.generationType())
                                                                              .symmetry(options.symmetry())
                                                                              .style(options.style())
                                                                              .terrainStyle(options.terrainStyle())
                                                                              .textureStyle(options.textureStyle())
                                                                              .resourceStyle(options.resourceStyle())
                                                                              .propStyle(options.propStyle())
                                                                              .reclaimDensity(options.reclaimDensity())
                                                                              .resourceDensity(
                                                                                  options.resourceDensity())
                                                                              .commandLineArgs(
                                                                                  options.commandLineArgs());

    try {
      List<String> command = commandBuilder.build().getCommand();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.directory(workingDirectory.toFile());
      processBuilder.command(command);

      log.info("Starting multiple map generator in directory: `{}` with command: `{}`", processBuilder.directory(),
               String.join(" ", processBuilder.command()));

      Process process = processBuilder.start();

      List<String> allLogLines = new ArrayList<>();
      OsUtils.gobbleLines(process.getInputStream(), msg -> {
        generatorLogger.info(msg);
        allLogLines.add(msg);
      });
      OsUtils.gobbleLines(process.getErrorStream(), generatorLogger::error);
      process.waitFor(MapGeneratorService.GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      if (process.isAlive()) {
        log.warn("Multiple map generation timed out, killing process");
        process.destroyForcibly();
        notificationService.addImmediateErrorNotification(new RuntimeException("Multiple map generation timed out"),
                                                          "game.mapGeneration.failed.message");
        return new ArrayList<>();
      }

      List<String> generatedMapNamesList = allLogLines.stream()
                                                      .map(MapGeneratorService.GENERATED_MAP_PATTERN::matcher)
                                                      .filter(Matcher::find)
                                                      .map(Matcher::group)
                                                      .distinct()
                                                      .toList();

      log.info("Successfully generated {} map(s): {}", generatedMapNamesList.size(), generatedMapNamesList);

      return generatedMapNamesList;
    } catch (Exception e) {
      log.error("Could not start multiple map generator", e);
      throw new RuntimeException(e);
    }
  }

}
