package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.util.Unzipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedInputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

public class DownloadMapTask extends PrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  I18n i18n;

  private String mapUrl;
  private String technicalMapName;

  public DownloadMapTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("mapDownloadTask.title", technicalMapName));
    logger.info("Downloading map {} from {}", technicalMapName, mapUrl);

    HttpURLConnection urlConnection = (HttpURLConnection) new URL(mapUrl).openConnection();
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

  public void setMapUrl(String mapUrl) {
    this.mapUrl = mapUrl;
  }

  public void setTechnicalMapName(String technicalMapName) {
    this.technicalMapName = technicalMapName;
  }
}
