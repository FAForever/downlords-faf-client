package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.util.ByteCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReplayDownloadTask extends PrioritizedTask<Path> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TEMP_FAF_REPLAY_FILE_NAME = "temp.fafreplay";

  @Autowired
  I18n i18n;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  private int replayId;

  public ReplayDownloadTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(i18n.get("mapReplayTask.title", replayId));

    String replayUrl = getReplayUrl(replayId, environment.getProperty("vault.replayDownloadUrl"));

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
