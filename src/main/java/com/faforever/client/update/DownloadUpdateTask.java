package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.util.ByteCopier;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadUpdateTask extends PrioritizedTask<Path> {

  private UpdateInfo updateInfo;
  private PreferencesService preferencesService;

  public DownloadUpdateTask(I18n i18n, PreferencesService preferencesService, UpdateInfo updateInfo) {
    super(i18n.get("clientUpdateDownloadTask.title"), Priority.MEDIUM);
    this.preferencesService = preferencesService;
    this.updateInfo = updateInfo;
  }

  @Override
  protected Path call() throws Exception {
    URL url = updateInfo.getUrl();

    Path path = preferencesService.getCacheDirectory().resolve("update").resolve(updateInfo.getFileName());

    Files.createDirectories(path.getParent());

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(path)) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(updateInfo.getSize())
          .listener(this::updateProgress)
          .copy();
    }

    return path;
  }
}
