package com.faforever.client.replay;


import com.faforever.commons.replay.ReplayData;

import java.nio.file.Path;

public interface ReplayFileReader {
  /**
   * Parses the actual replay data of the specified file and returns metadata, raw data, chat messages, game options,
   * executed commands and so on.
   */
  ReplayData parseReplay(Path path);
}
