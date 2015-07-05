package com.faforever.client.replay;

import java.nio.file.Path;

public interface ReplayFileReader {

  /**
   * Returns the meta information about this replay.
   */
  LocalReplayInfo readReplayInfo(Path replayFile);

  /**
   * Returns the binary replay data.
   */
  byte[] readReplayData(Path replayFile);
}
