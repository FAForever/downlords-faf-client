package com.faforever.client.discord;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Registers the {@code discord-<applicationId>://} URL protocol so Discord can start the client when the user joins or
 * spectates via Discord while the client isn't running. Mirrors {@code Discord_Register} of the deprecated discord-rpc
 * library.
 */
@Slf4j
final class DiscordLaunchRegistration {

  private static final Set<String> JAVA_EXECUTABLES = Set.of("java.exe", "javaw.exe");

  @FunctionalInterface
  interface Registry {
    /** Sets a string value of a key below {@code HKEY_CURRENT_USER}, creating the key if needed. */
    void setValue(String key, String name, String value);
  }

  private DiscordLaunchRegistration() {
  }

  static void registerOnWindows(String applicationId) {
    register(applicationId, ProcessHandle.current().info().command().map(Path::of), (key, name, value) -> {
      Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, key);
      Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, key, name, value);
    });
  }

  static void register(String applicationId, Optional<Path> executable, Registry registry) {
    if (executable.isEmpty()) {
      log.info("Not registering Discord launch command, executable is unknown");
      return;
    }
    Path executablePath = executable.get();
    if (JAVA_EXECUTABLES.contains(executablePath.getFileName().toString().toLowerCase())) {
      log.info("Not registering Discord launch command, not started by the launcher: {}", executablePath);
      return;
    }

    String key = "Software\\Classes\\discord-" + applicationId;
    registry.setValue(key, "", "URL:Run game " + applicationId + " protocol");
    registry.setValue(key, "URL Protocol", "");
    registry.setValue(key + "\\DefaultIcon", "", executablePath.toString());
    registry.setValue(key + "\\shell\\open\\command", "", "\"" + executablePath + "\"");
    log.info("Registered Discord launch command: {}", executablePath);
  }
}
