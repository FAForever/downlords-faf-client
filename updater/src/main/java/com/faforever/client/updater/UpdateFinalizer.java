package com.faforever.client.updater;

import org.update4j.Archive;
import org.update4j.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateFinalizer {
  public static void main(String[] args) throws Exception {
    Path updateArchive = Paths.get(args[0]);
    if (Files.notExists(updateArchive)) {
      System.out.println(updateArchive + " doesn't exist");
      return;
    }

    String pid = args[1];
    waitFor(pid);

    System.out.println("Finalizing update");

    // FIXME rename downlords-faf-client.exe to downlords-faf-client.exe_ to prevent the user from starting while we're updating
    Archive.read(updateArchive).install();

    Configuration newConfig = readConfiguration(updateArchive.resolveSibling("update4j-new.xml"));
    Path oldConfigFile = updateArchive.resolveSibling("update4j-old.xml");
    if (Files.exists(oldConfigFile)) {
      Configuration oldConfig = readConfiguration(oldConfigFile);
      newConfig.deleteOldFiles(oldConfig);
    }

    UninstallInformationUpdateService.update(newConfig);

    // FIXME rename downlords-faf-client.exe_ to downlords-faf-client.exe
  }

  private static Configuration readConfiguration(Path file) throws IOException {
    try (Reader reader = Files.newBufferedReader(file)) {
      return Configuration.read(reader);
    }
  }

  private static void waitFor(String pid) throws Exception {
    System.out.println("Waiting for PID " + pid + " to terminate");
    while (isProcessRunning(pid)) {
      Thread.sleep(1000);
    }
  }

  private static boolean isProcessRunning(String pid) throws Exception {
    Process process = System.getProperty("os.name").startsWith("Windows")
      ? Runtime.getRuntime().exec("tasklist /FI \"PID eq " + pid + "\"")
      : Runtime.getRuntime().exec("ps -p " + pid);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      return reader.lines().anyMatch(s -> s.contains(pid));
    }
  }
}
