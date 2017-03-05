package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.io.Unzipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipInputStream;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InstallModTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;
  private final I18n i18n;

  private URL url;

  @Inject
  public InstallModTask(PreferencesService preferencesService, I18n i18n) {
    super(HIGH);

    this.preferencesService = preferencesService;
    this.i18n = i18n;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(url, "url has not been set");

    Path tempFile = Files.createTempFile(preferencesService.getCacheDirectory(), "mod", null);

    logger.info("Downloading mod {} to {}", url, tempFile);
    updateTitle(i18n.get("downloadingModTask.downloading", url));

    Files.createDirectories(tempFile.getParent());

    URLConnection urlConnection = url.openConnection();
    int contentLength = urlConnection.getContentLength();

    try (InputStream inputStream = urlConnection.getInputStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ResourceLocks.acquireDownloadLock();

      ByteCopier.from(inputStream)
          .to(outputStream)
          .listener(this::updateProgress)
          .totalBytes(contentLength)
          .copy();

      extractMod(tempFile);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }
    return null;
  }

  private void extractMod(Path tempFile) throws IOException {
    Path modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();

    updateTitle(i18n.get("downloadingModTask.unzipping", modsDirectory));
    logger.info("Unzipping {} to {}", tempFile, modsDirectory);

    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(tempFile))) {
      ResourceLocks.acquireDiskLock();

      Unzipper.from(zipInputStream)
          .to(modsDirectory)
          .listener(this::updateProgress)
          .totalBytes(Files.size(tempFile))
          .unzip();

    } finally {
      ResourceLocks.freeDiskLock();
    }
  }

  public void setUrl(URL url) {
    this.url = url;
  }
}
