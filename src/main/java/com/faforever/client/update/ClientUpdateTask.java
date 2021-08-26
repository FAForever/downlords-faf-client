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
import org.update4j.UpdateOptions;
import org.update4j.UpdateOptions.ArchiveUpdateOptions;
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
import java.util.concurrent.CompletableFuture;

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

    Path updateFile = preferencesService.getCacheDirectory().resolve("update.zip");
    FileUtils.deleteRecursively(updateFile);
    Files.createDirectories(updateFile.getParent());

    Configuration newConfiguration = updateInfo.getNewConfiguration();

    update(updateFile, newConfiguration);

    writeConfiguration(newConfiguration, updateFile.resolveSibling("update4j-new.xml"));

    updateInfo.getCurrentConfiguration().ifPresentOrElse(
      configuration -> writeConfiguration(configuration, updateFile.resolveSibling("update4j-old.xml")),
      () -> log.warn("Old configuration could not be determined, files won't be uninstalled.")
    );

    startUpdateFinalizer(updateFile);

    return null;
  }

  private void update(Path updateFile, Configuration newConfiguration) throws InterruptedException, java.util.concurrent.ExecutionException {
    CompletableFuture<Void> future = new CompletableFuture<>();
    ArchiveUpdateOptions options = UpdateOptions.archive(updateFile).updateHandler(updateHandler(future));
    newConfiguration.update(options);

    future.get();
  }

  @SneakyThrows
  private void writeConfiguration(Configuration configuration, Path file) {
    try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      configuration.write(writer);
    }
  }

  @SneakyThrows
  private void startUpdateFinalizer(Path updateFile) {
    final Path updateDirectory = updateFile.getParent();
    Path jreDir = copyJre(updateDirectory);
    Path updaterJar = copyUpdaterJar(updateDirectory);
    if (Files.notExists(updateFile)) {
      log.warn("Update file does not exist. This should only happen when the client is up to date: {}", updateFile);
      return;
    }

    String command = String.format("%s -Xmx5m -jar %s %s %d > nul 2>&1",
      jreDir.resolve("bin/java").toAbsolutePath(),
      updaterJar.toAbsolutePath(),
      updateFile,
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
    Path updaterJar = Paths.get(System.getProperty("externalToolsDir", "tools")).resolve("updater.jar");
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

  private UpdateHandler updateHandler(CompletableFuture<Void> future) {
    return new UpdateHandler() {
      @Override
      public void failed(Throwable t) {
        log.warn("Update failed", t);
        future.completeExceptionally(t);
      }

      @Override
      public void succeeded() {
        future.complete(null);
      }

      @Override
      public void updateDownloadProgress(float frac) {
        updateProgress(frac, 1);
      }
    };
  }
}
