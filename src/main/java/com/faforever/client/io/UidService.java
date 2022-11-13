package com.faforever.client.io;

import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Lazy
@Service
@RequiredArgsConstructor
public class UidService {
  private final OperatingSystem operatingSystem;

  public String generate(String sessionId) throws IOException {
    Path uidPath = operatingSystem.getUidExecutablePath();
    return OsUtils.execAndGetOutput(uidPath.toAbsolutePath().toString(), sessionId);
  }
}
