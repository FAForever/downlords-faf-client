package com.faforever.client.replay;

import com.faforever.commons.replay.ReplayData;
import com.faforever.commons.replay.ReplayDataParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Lazy
@Component
@Slf4j
public class ReplayFileReaderImpl implements ReplayFileReader {

  @Override
  public ReplayData parseReplay(Path path) {
    return new ReplayDataParser(path).parse();
  }
}
