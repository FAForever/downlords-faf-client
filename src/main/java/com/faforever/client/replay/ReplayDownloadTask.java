package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.commons.io.ByteCopier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ReplayDownloadTask extends CompletableTask<Path> {

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

    String replayUrl = ReplayBean.getReplayUrl(replayId, clientProperties.getVault().getReplayDownloadUrlFormat());

    log.info("Downloading replay {} from {}", replayId, replayUrl);

    HttpURLConnection urlConnection = (HttpURLConnection) new URL(replayUrl).openConnection();
    urlConnection.setInstanceFollowRedirects(true);
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


  public void setReplayId(int replayId) {
    this.replayId = replayId;
  }
}
