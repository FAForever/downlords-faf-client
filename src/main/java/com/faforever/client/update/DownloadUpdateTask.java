package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.commons.io.ByteCopier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class DownloadUpdateTask extends CompletableTask<Path> {

  private final I18n i18n;
  private final DataPrefs dataPrefs;

  private UpdateInfo updateInfo;

  @Autowired
  public DownloadUpdateTask(I18n i18n, DataPrefs dataPrefs) {
    super(Priority.MEDIUM);

    this.i18n = i18n;
    this.dataPrefs = dataPrefs;
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));
    URL url = updateInfo.url();

    Path updateDirectory = dataPrefs.getCacheDirectory().resolve("update");
    Path targetFile = updateDirectory.resolve(updateInfo.fileName());
    Files.createDirectories(targetFile.getParent());

    Path tempFile = Files.createTempFile(targetFile.getParent(), "update", null);

      ResourceLocks.acquireDownloadLock();
    try (InputStream inputStream = url.openStream(); OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ByteCopier.from(inputStream)
          .to(outputStream)
                .totalBytes(updateInfo.size())
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }

    return targetFile;
  }

  public void setUpdateInfo(UpdateInfo updateInfo) {
    this.updateInfo = updateInfo;
  }
}
