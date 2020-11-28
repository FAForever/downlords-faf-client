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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GenerateMapTask extends CompletableTask<Void> {
  private static final Logger generatorLogger = LoggerFactory.getLogger("faf-map-generator");

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Setter
  private String version;
  @Setter
  private String seed;
  @Setter
  private Path generatorExecutableFile;
  @Setter
  private String mapFilename;

  @Inject
  public GenerateMapTask(PreferencesService preferencesService, NotificationService notificationService, I18n i18n, EventBus eventBus) {
    super(Priority.HIGH);

    this.preferencesService = preferencesService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");

    updateTitle(i18n.get("game.mapGeneration.generateMap.title", version, String.valueOf(seed)));

    Path workingDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(workingDirectory.toFile());
    processBuilder.command("java", "-jar", generatorExecutableFile.toAbsolutePath().toString(), ".", String.valueOf(seed), version, mapFilename);
    processBuilder.environment().put("LOG_DIR", preferencesService.getFafLogDirectory().toAbsolutePath().toString());

    log.info("Starting map generator in directory: {} with command: {}",
        processBuilder.directory(), processBuilder.command().stream().reduce((l, r) -> l + " " + r).get());
    try {
      Process process = processBuilder.start();
      OsUtils.gobbleLines(process.getInputStream(), generatorLogger::info);
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
    } catch (IOException | InterruptedException e) {
      log.error("Could not start map generator.", e);
      throw new RuntimeException(e);
    }

    return null;
  }
}
