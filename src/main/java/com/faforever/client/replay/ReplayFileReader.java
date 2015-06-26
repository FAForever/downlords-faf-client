package com.faforever.client.replay;

import java.nio.file.Path;

public interface ReplayFileReader {

  ReplayInfo readReplayFile(Path replayFile);
}
