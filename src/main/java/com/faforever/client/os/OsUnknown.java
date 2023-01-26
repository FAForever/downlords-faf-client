package com.faforever.client.os;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@Slf4j
public final class OsUnknown implements OperatingSystem {
  @Override
  public boolean runsAsAdmin() {
    log.warn("Unsupported operating system can't detect whether it's run as admin.");
    return false;
  }

  @Override
  public boolean supportsUpdateInstall() {
    return false;
  }

  @Override
  @NotNull
  public Path getLoggingDirectory() {
    throw new NotImplementedException("Cannot derive logging directory in unsupported OS");
  }

  @Override
  @NotNull
  public Path getPreferencesDirectory() {
    throw new NotImplementedException("Cannot derive preferences directory in unsupported OS");
  }

  @Override
  @NotNull
  public Path getUidExecutablePath() {
    throw new NotImplementedException("Cannot derive uid binary in unsupported OS");
  }

  @Override
  @NotNull
  public Path getJavaExecutablePath() {
    throw new NotImplementedException("Cannot derive java binary name in unsupported OS");
  }

  @Override
  @NotNull
  public String getGithubAssetFileEnding() {
    throw new NotImplementedException("Cannot derive github asset file ending in unsupported OS");
  }

  @Override
  public @NotNull Path getDefaultDataDirectory() {
    throw new NotImplementedException("Cannot derive data directory in unsupported OS");
  }

  @Override
  public @NotNull Path getSteamFaDirectory() {
    throw new NotImplementedException("Cannot derive steam fa in unsupported OS");

  }

  @Override
  public @NotNull Path getLocalFaDataPath() {
    throw new NotImplementedException("Cannot derive local fa directory in unsupported OS");

  }

  @Override
  public @NotNull Path getDefaultVaultDirectory() {
    throw new NotImplementedException("Cannot vault directory in unsupported OS");
  }
}
