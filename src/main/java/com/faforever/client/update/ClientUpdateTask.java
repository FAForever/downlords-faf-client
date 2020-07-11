package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.Configuration;
import org.update4j.service.UpdateHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    FileUtils.deleteRecursively(updateDirectory);

    Configuration newConfiguration = updateInfo.getNewConfiguration();
    newConfiguration.updateTemp(updateDirectory, updateHandler());

    writeConfiguration(newConfiguration, updateDirectory.resolve("update4j-new.xml"));
    updateInfo.getCurrentConfiguration()
        .ifPresent(configuration -> writeConfiguration(configuration, updateDirectory.resolve("update4j-old.xml")));

    startUpdateFinalizer(updateDirectory);

    return null;
  }

  @SneakyThrows
  private void writeConfiguration(Configuration configuration, Path file) {
    try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      configuration.write(writer);
    }
  }

  @SneakyThrows
  private void startUpdateFinalizer(Path updateDirectory) {
    Path jreDir = copyJre(updateDirectory);
    Path updaterJar = copyUpdaterJar(updateDirectory);

    String command = String.format("%s -Xmx5m -jar %s %s %d",
        jreDir.resolve("bin/java").toAbsolutePath(),
        updaterJar.toAbsolutePath(),
        updateDirectory,
        ManagementFactory.getRuntimeMXBean().getPid());
    log.info("Starting update finalizer using command: {}", command);
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
    return tmpJar;
  }

  @SneakyThrows
  private URL getUpdaterJar() {
    Path updaterJar = Paths.get(System.getProperty("externalToolsDir", "lib")).resolve("updater.jar");
    if (Files.exists(updaterJar)) {
      return updaterJar.toUri().toURL();
    }

    // For development environments
    Path path = Paths.get("build/resources/updater/updater.jar");
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
