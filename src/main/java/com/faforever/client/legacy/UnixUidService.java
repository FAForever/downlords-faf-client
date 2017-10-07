package com.faforever.client.legacy;

import com.faforever.client.os.OsUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;


@Lazy
@Service
@Profile({"linux", "mac"})
public class UnixUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) throws IOException {
    String uidDir = System.getProperty("nativeDir", "lib");
    return OsUtils.execAndGetOutput(String.format("%s/faf-uid %s", uidDir, sessionId));
  }
}
