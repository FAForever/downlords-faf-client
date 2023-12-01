package com.faforever.client.os;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public sealed interface OperatingSystem permits OsPosix, OsUnknown, OsWindows {

  boolean runsAsAdmin();

  boolean supportsUpdateInstall();

  @NotNull Path getLoggingDirectory();

  @NotNull Path getPreferencesDirectory();

  @NotNull Path getUidExecutablePath();

  @NotNull Path getJavaExecutablePath();

  @NotNull String getGithubAssetFileEnding();

  @NotNull Path getDefaultDataDirectory();

  Path getSteamFaDirectory();

  @NotNull Path getLocalFaDataPath();

  @NotNull Path getDefaultVaultDirectory();

  void increaseProcessPriority(Process process);


  /**
   * The character to separate different paths in a list e.g. for list of classpath files (as String).
   * <p>
   * On UNIX systems, this character is {@code ':'}; on Microsoft Windows systems it is {@code ';'}.
   */
  default String getPathListSeparator() {
    return File.pathSeparator;
  }

  /**
   * The character to separate different folders in a path (as String).
   * <p>
   * On UNIX systems the value of this field is {@code '/'}; on Microsoft Windows systems it is {@code '\\'}.
   */
  default String getPathSeparator() {
    return File.separator;
  }
}
