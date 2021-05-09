package com.faforever.client.replay;

import com.faforever.commons.replay.ReplayDataParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Lazy
@Component
@Slf4j
public class ReplayFileReaderImpl implements ReplayFileReader {

  private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public ReplayDataParser parseReplay(Path path) throws IOException, CompressorException {
    return new ReplayDataParser(path, objectMapper);
  }
}
