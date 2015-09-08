package com.faforever.client.replay;

import com.faforever.client.legacy.io.QtCompress;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public LocalReplayInfo readReplayInfo(Path replayFile) throws IOException {
    logger.debug("Reading replay file {}", replayFile);
    List<String> lines = Files.readAllLines(replayFile);
    return gson.fromJson(lines.get(0), LocalReplayInfo.class);
  }

  @Override
  public byte[] readReplayData(Path replayFile) {
    logger.debug("Reading replay file {}", replayFile);
    try {
      List<String> lines = Files.readAllLines(replayFile);
      return QtCompress.qUncompress(BaseEncoding.base64().decode(lines.get(1)));
    } catch (Exception e) {
      logger.warn("Replay file " + replayFile + " could not be read", e);
      return null;
    }
  }
}
