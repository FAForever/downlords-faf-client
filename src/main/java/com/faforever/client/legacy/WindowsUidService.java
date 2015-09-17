package com.faforever.client.legacy;

import com.faforever.client.util.UID;

import java.nio.file.Path;

public class WindowsUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    return UID.generate(sessionId, logFile);
  }
}
