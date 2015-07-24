package com.faforever.client.replay;

import java.io.IOException;
import java.nio.file.Path;

public interface ReplayFileReader {

  /**
   * Returns the meta information about this replay.
   */
  LocalReplayInfo readReplayInfo(Path replayFile) throws IOException;

  /**
   * Returns the binary replay data.
   */
  byte[] readReplayData(Path replayFile);
}
