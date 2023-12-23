package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.PrioritizedCompletableTask;
import com.faforever.commons.io.Unzipper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownloadMapTask extends PrioritizedCompletableTask<Void> {

  private final I18n i18n;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private URL mapUrl;
  private String folderName;

  @Autowired
  public DownloadMapTask(I18n i18n, ForgedAlliancePrefs forgedAlliancePrefs) {
    super(Priority.HIGH);

    this.forgedAlliancePrefs = forgedAlliancePrefs;
    this.i18n = i18n;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(mapUrl, "mapUrl has not been set");
    Objects.requireNonNull(folderName, "folderName has not been set");

    updateTitle(i18n.get("mapDownloadTask.title", folderName));
    log.info("Downloading map `{}` from `{}`", folderName, mapUrl);

    URLConnection urlConnection = mapUrl.openConnection();
    int bytesToRead = urlConnection.getContentLength();

    Path targetDirectory = forgedAlliancePrefs.getMapsDirectory();

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
