package com.faforever.client.legacy;

import com.faforever.client.os.OsUtils;

import java.nio.file.Path;

public class UnixUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    String uidDir = System.getProperty("uid.dir", "lib");
    return OsUtils.execAndGetOutput(String.format("/bin/sh -C %s/uid %s", uidDir, sessionId));
  }
}
