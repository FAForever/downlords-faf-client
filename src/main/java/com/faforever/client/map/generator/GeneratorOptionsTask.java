package com.faforever.client.map.generator;

import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OsUtils;
import com.faforever.client.task.CompletableTask;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Setter
public class GeneratorOptionsTask extends CompletableTask<List<String>> {
  private static final Logger generatorLogger = LoggerFactory.getLogger("faf-map-generator");

  private final I18n i18n;

  private ComparableVersion version;
  private Path generatorExecutableFile;
  private String query;
  private List<String> options;

  @Inject
  public GeneratorOptionsTask(I18n i18n) {
    super(Priority.HIGH);

    this.i18n = i18n;
  }

  @Override
  protected List<String> call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");

    options = new ArrayList<>();

    updateTitle(i18n.get("game.mapGeneration.options.title", version));

    GeneratorCommand generatorCommand = GeneratorCommand.builder()
        .version(version)
        .generatorExecutableFile(generatorExecutableFile)
        .query(query)
        .build();

    try {
      List<String> command = generatorCommand.getCommand();

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command(command);

      log.info("Starting map generator in directory: {} with command: {}",
          processBuilder.directory(), String.join(" ", processBuilder.command()));

      Process process = processBuilder.start();
      OsUtils.gobbleLines(process.getInputStream(), msg -> {
        if (!msg.contains(":")) {
          options.add(msg);
        }
      });
      OsUtils.gobbleLines(process.getErrorStream(), generatorLogger::error);
      process.waitFor(2, TimeUnit.SECONDS);
      if (process.isAlive()) {
        process.destroyForcibly();
        log.warn("Map generator option run timed out");
      }
    } catch (Exception e) {
      log.error("Could not start map generator.", e);
      throw new RuntimeException(e);
    }

    return options;
  }
}
