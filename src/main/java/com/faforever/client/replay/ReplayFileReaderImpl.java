package com.faforever.client.replay;

import com.faforever.client.notification.NotificationService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReplayFileReaderImpl implements ReplayFileReader {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;

  public ReplayFileReaderImpl() {
    gson = ReplayFiles.gson();
  }

  @Override
  public ReplayInfo readReplayFile(Path replayFile) {
    try {
      List<String> lines = Files.readAllLines(replayFile);

      return gson.fromJson(lines.get(0), ReplayInfo.class);
    } catch (IOException e) {
      logger.warn("Replay file " + replayFile + " could not be read", e);
      return null;
    }
  }
}
