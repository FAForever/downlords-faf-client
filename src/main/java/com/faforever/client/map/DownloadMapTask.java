package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.Unzipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipInputStream;

public class DownloadMapTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  PreferencesService preferencesService;
  @Resource
  I18n i18n;

  private URL mapUrl;
  private String folderName;

  public DownloadMapTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(mapUrl, "mapUrl has not been set");
    Objects.requireNonNull(folderName, "folderName has not been set");

    updateTitle(i18n.get("mapDownloadTask.title", folderName));
    logger.info("Downloading map {} from {}", folderName, mapUrl);

    URLConnection urlConnection = mapUrl.openConnection();
    int bytesToRead = urlConnection.getContentLength();

    Path targetDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();

    try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(urlConnection.getInputStream()))) {
      Unzipper.from(inputStream)
          .to(targetDirectory)
          .totalBytes(bytesToRead)
          .listener(this::updateProgress)
          .unzip();
    }

    return null;
  }

  public void setMapUrl(URL mapUrl) {
    this.mapUrl = mapUrl;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }
}
