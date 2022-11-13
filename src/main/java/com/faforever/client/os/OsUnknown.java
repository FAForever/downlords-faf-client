package com.faforever.client.os;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@Slf4j
public class OsUnknown implements OperatingSystem {
  @Override
  public boolean runsAsAdmin() {
    log.warn("Unknown operating system can't detect whether it's run as admin.");
    return false;
  }

  @Override
  public boolean supportsUpdateInstall() {
    return false;
  }

  @Override
  public @NotNull Path getUidExecutablePath() {
    throw new NotImplementedException("Cannot derive uid binary in unsupported OS");
  }

  @Override
  public @NotNull Path getJavaExecutablePath() {
    throw new NotImplementedException("Cannot derive java binary name in unsupported OS");
  }
}
