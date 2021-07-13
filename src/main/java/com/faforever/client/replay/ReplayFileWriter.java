package com.faforever.client.replay;

import com.faforever.commons.replay.ReplayMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ReplayFileWriter {

  void writeReplayDataToFile(ByteArrayOutputStream replayData, ReplayMetadata replayInfo) throws IOException;
}
