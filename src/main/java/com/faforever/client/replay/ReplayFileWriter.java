package com.faforever.client.replay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ReplayFileWriter {

  void writeReplayDataToFile(ByteArrayOutputStream replayData, LocalReplayInfo replayInfo) throws IOException;
}
