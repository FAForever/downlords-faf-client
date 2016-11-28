package com.faforever.client.legacy;

import com.faforever.client.os.OsUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;


@Lazy
@Service
@Profile("windows")
public class WindowsUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) {
    String uidDir = System.getProperty("uid.dir", "lib");
    return OsUtils.execAndGetOutput(String.format("%s/uid.exe %s", uidDir, sessionId));
  }
}
