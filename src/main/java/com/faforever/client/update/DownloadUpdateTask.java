package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.ByteCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadUpdateTask extends AbstractPrioritizedTask<Path> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  I18n i18n;

  @Autowired
  PreferencesService preferencesService;

  private UpdateInfo updateInfo;

  public DownloadUpdateTask() {
    super(Priority.MEDIUM);
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));
    URL url = updateInfo.getUrl();

    Path targetFile = preferencesService.getCacheDirectory().resolve("update").resolve(updateInfo.getFileName());
    Files.createDirectories(targetFile.getParent());

    Path tempFile = Files.createTempFile(targetFile.getParent(), "update", null);

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(targetFile)) {
      ResourceLocks.aquireDownloadLock();
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(updateInfo.getSize())
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }

    return targetFile;
  }

  public void setUpdateInfo(UpdateInfo updateInfo) {
    this.updateInfo = updateInfo;
  }
}
