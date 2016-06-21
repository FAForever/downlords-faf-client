package com.faforever.client.legacy;

import com.faforever.client.os.OsUtils;

import java.nio.file.Path;

public class UnixUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    return OsUtils.execAndGetOutput("uid " + sessionId);
  }
}
