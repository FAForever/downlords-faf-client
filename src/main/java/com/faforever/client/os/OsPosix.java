package com.faforever.client.os;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public class OsPosix implements OperatingSystem {
  @Override
  public boolean runsAsAdmin() {
    String username = System.getProperty("user.name");
    return Objects.equals(username, "root");
  }

  @Override
  public boolean supportsUpdateInstall() {
    // The automatic download and installation of update doesn't work on Linux as there is no unified installer
    return false;
  }

  @Override
  @NotNull
  public Path getUidExecutablePath() {
    String uidDir = System.getProperty("nativeDir", "lib");
    return Path.of(uidDir).resolve("faf-uid");
  }

  @Override
  public @NotNull Path getJavaExecutablePath() {
    return Path.of(System.getProperty("java.home"))
        .resolve("bin")
        .resolve("java");
  }

  @Override
  public @NotNull String getGithubAssetFileEnding() {
    return ".tar.gz";
  }
}
