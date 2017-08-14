package com.faforever.client.replay;

import com.faforever.commons.replay.QtCompress;
import com.faforever.commons.replay.ReplayData;
import com.faforever.commons.replay.ReplayDataParser;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Lazy
@Component
@Slf4j
public class ReplayFileReaderImpl implements ReplayFileReader {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Gson gson;

  public ReplayFileReaderImpl() {
    gson = ReplayFiles.gson();
  }

  @Override
  @SneakyThrows
  public LocalReplayInfo parseMetaData(Path replayFile) {
    logger.debug("Parsing metadata of replay file: {}", replayFile);
    List<String> lines = Files.readAllLines(replayFile);
    return gson.fromJson(lines.get(0), LocalReplayInfo.class);
  }

  @Override
  @SneakyThrows
  public byte[] readRawReplayData(Path replayFile) {
    logger.debug("Reading replay file: {}", replayFile);
    List<String> lines = Files.readAllLines(replayFile);
    return QtCompress.qUncompress(BaseEncoding.base64().decode(lines.get(1)));
  }

  @Override
  public ReplayData parseReplay(Path path) {
    return new ReplayDataParser(path).parse();
  }
}
