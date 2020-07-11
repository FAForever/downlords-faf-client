package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.updater.Updater;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.service.UpdateHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ClientUpdateTask extends CompletableTask<Void> {

  private final I18n i18n;
  private final PreferencesService preferencesService;

  @Setter
  private UpdateInfo updateInfo;

  @Inject
  public ClientUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.MEDIUM);

    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));

    // update4j will check for an .update file
    Path updateDirectory = preferencesService.getCacheDirectory().resolve("update");

    updateInfo.getConfiguration().updateTemp(updateDirectory, updateHandler());

    startUpdateFinalizer(updateDirectory);

    return null;
  }

  @SneakyThrows
  private void startUpdateFinalizer(Path updateDirectory) {
    Path jreDir = copyJre(updateDirectory);
    Path updaterJar = copyUpdaterJar(updateDirectory);

    String command = String.format("%s -jar %s", jreDir.resolve("bin/java").toAbsolutePath(), updaterJar.toAbsolutePath());
    log.info("Starting updater using command: {}", command);
    Process exec = Runtime.getRuntime().exec(command);
    log.info("Updater pid is {}", exec.pid());
  }

  private Path copyJre(Path updateDirectory) {
    Path jreSourceDir = Paths.get(System.getProperty("java.home"));
    Path jreTargetDir = updateDirectory.resolve("jre");

    log.debug("Copying JRE from {} to {}", jreSourceDir, jreTargetDir);

    FileUtils.copyContentRecursively(jreSourceDir, jreTargetDir);
    return jreTargetDir;
  }

  private Path copyUpdaterJar(Path updateDirectory) throws IOException, URISyntaxException {
    Path tmpJar = updateDirectory.resolve("updater.jar");
    Path sourceJar = Paths.get(getUpdaterJar().toURI());

    // Since the updater will delete the updater JAR, it must not be run from it.
    log.debug("Copying updater JAR from {} to {}", sourceJar, tmpJar);
    Files.copy(sourceJar, tmpJar, StandardCopyOption.REPLACE_EXISTING);
    return sourceJar;
  }

  @SneakyThrows
  private URL getUpdaterJar() {
    URL location = Updater.class.getProtectionDomain().getCodeSource().getLocation();
    if (location.toExternalForm().endsWith(".jar")) {
      return location;
    }

    // For development only, where there is no JAR
    Path path = Paths.get("updater/build/libs/updater-unspecified.jar");
    if (Files.notExists(path)) {
      throw new IllegalStateException("In order to use the updater during development, you need to build the updater.jar");
    }
    return path.toUri().toURL();
  }

  private UpdateHandler updateHandler() {
    return new UpdateHandler() {
      @Override
      public void updateDownloadProgress(float frac) {
        updateProgress(frac, 1);
      }
    };
  }
}
