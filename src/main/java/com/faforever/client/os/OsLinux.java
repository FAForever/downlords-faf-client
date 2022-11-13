package com.faforever.client.os;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public class OsLinux implements OperatingSystem {
  @Override
  public boolean runsAsAdmin() {
    var username = System.getProperty("user.name");
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
}
