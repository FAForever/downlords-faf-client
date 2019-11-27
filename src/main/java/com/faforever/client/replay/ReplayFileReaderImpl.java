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

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.nocatch.NoCatch.noCatch;

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
    try (Stream<String> stream = Files.lines(replayFile)) {
      return stream
          .findFirst()
          .map(jsonString -> gson.fromJson(jsonString, LocalReplayInfo.class))
          .orElseThrow(() -> new IOException(String.format("Failed to extract metadata from replay file: {}", replayFile)));
    }
  }

  @Override
  @SneakyThrows
  public byte[] readRawReplayData(Path replayFile) {
    logger.debug("Reading replay file: {}", replayFile);
    try (Stream<String> stream = Files.lines(replayFile)) {
      return stream
          .skip(1)
          .findFirst()
          .map(base64String -> noCatch(() -> QtCompress.qUncompress(BaseEncoding.base64().decode(base64String))))
          .orElseThrow(() -> new IOException(String.format("Failed to extract replay data from replay file: {}", replayFile)));
    }
  }

  @Override
  public ReplayData parseReplay(Path path) {
    return new ReplayDataParser(path).parse();
  }
}
