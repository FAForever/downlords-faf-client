package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayDownloadTask extends CompletableTask<Path> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TEMP_FAF_REPLAY_FILE_NAME = "temp.fafreplay";

  private final I18n i18n;
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;

  private int replayId;

  @Inject
  public ReplayDownloadTask(I18n i18n, ClientProperties clientProperties, PreferencesService preferencesService) {
    super(Priority.HIGH);

    this.i18n = i18n;
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(i18n.get("mapReplayTask.title", replayId));

    String replayUrl = getReplayUrl(replayId, clientProperties.getVault().getReplayDownloadUrlFormat());

    logger.info("Downloading replay {} from {}", replayId, replayUrl);

    HttpURLConnection urlConnection = (HttpURLConnection) new URL(replayUrl).openConnection();
    int bytesToRead = urlConnection.getContentLength();

    Path tempSupComReplayFile = preferencesService.getCacheDirectory().resolve(TEMP_FAF_REPLAY_FILE_NAME);

    Files.createDirectories(tempSupComReplayFile.getParent());

    try (InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
         OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempSupComReplayFile))) {

      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(bytesToRead)
          .listener(this::updateProgress)
          .copy();

      return tempSupComReplayFile;
    }
  }

  private String getReplayUrl(int replayId, String baseUrl) {
    return String.format(baseUrl, replayId);
  }

  public void setReplayId(int replayId) {
    this.replayId = replayId;
  }
}
