package com.faforever.client.legacy;

import java.nio.file.Path;

public class UnixUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    throw new UnsupportedOperationException("UID on linux is not yet supported");
  }
}
