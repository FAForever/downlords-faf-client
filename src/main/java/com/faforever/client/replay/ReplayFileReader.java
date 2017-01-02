package com.faforever.client.replay;


import com.faforever.commons.replay.ReplayData;

import java.nio.file.Path;

public interface ReplayFileReader {

  /**
   * Returns the meta information about this replay (its FAF header)
   */
  LocalReplayInfo parseMetaData(Path replayFile);

  /**
   * Returns the binary replay data.
   */
  byte[] readRawReplayData(Path replayFile);

  /**
   * Parses the actual replay data of the specified file and returns information such as chat messages, game options,
   * executed commands and so on.
   */
  ReplayData parseReplay(Path path);
}
