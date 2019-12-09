package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.commons.io.Unzipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownloadMapTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;
  private final I18n i18n;

  private URL mapUrl;
  private String folderName;

  @Inject
  public DownloadMapTask(PreferencesService preferencesService, I18n i18n) {
    super(Priority.HIGH);

    this.preferencesService = preferencesService;
    this.i18n = i18n;
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

    try (InputStream inputStream = urlConnection.getInputStream()) {
      Unzipper.from(inputStream)
          .zipBombByteCountThreshold(100_000_000)
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
