package com.faforever.client.os;

import com.faforever.client.preferences.PreferencesService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

import static com.faforever.client.preferences.PreferencesService.USER_HOME_SUB_FOLDER;

@Slf4j
public final class OsPosix implements OperatingSystem {
  @Override
  public boolean runsAsAdmin() {
    String username = System.getProperty("user.name");
    return Objects.equals(username, "root");
  }

  @Override
  public boolean supportsUpdateInstall() {
    return false;
  }

  @Override
  public @NotNull Path getLoggingDirectory() {
    return Path.of(System.getProperty("user.home")).resolve(PreferencesService.USER_HOME_SUB_FOLDER).resolve("logs");
  }

  @Override
  @NotNull
  public Path getPreferencesDirectory() {
    return Path.of(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
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

  @Override
  public @NotNull Path getDefaultDataDirectory() {
    return Path.of(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
  }

  @Override
  public Path getSteamFaDirectory() {
    return null;
  }

  @Override
  public @NotNull Path getLocalFaDataPath() {
    return Path.of(System.getProperty("user.home"), ".wine", "drive_c", "users", System.getProperty("user.name"), "Application Data", "Gas Powered Games", "Supreme Commander Forged Alliance");
  }

  @Override
  public @NotNull Path getDefaultVaultDirectory() {
    return Path.of(System.getProperty("user.home"), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
  }

  @Override
  public void increaseProcessPriority(Process process) {
    log.debug("Increasing process priority is not implemented for Posix. Ignoring request.");
  }

}
