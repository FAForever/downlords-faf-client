package com.faforever.client.legacy;

import com.faforever.client.os.OsUtils;

import java.nio.file.Path;

public class WindowsUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    String uidDir = System.getProperty("uid.dir");
    return OsUtils.execAndGetOutput(String.format("%s/uid.exe %s", uidDir, sessionId));
  }
}
