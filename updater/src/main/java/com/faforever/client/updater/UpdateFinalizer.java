package com.faforever.client.updater;

import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.Update;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

import static java.nio.file.Files.walkFileTree;

public class UpdateFinalizer {
  public static void main(String[] args) throws Exception {
    Path updateArchive = Paths.get(args[0]);
    if (!Files.exists(updateArchive)) {
      System.out.println(updateArchive + " doesn't exist");
      return;
    }

    String pid = args[1];
    waitFor(pid);

    System.out.println("Finalizing update");

    // FIXME rename downlords-faf-client.exe to downlords-faf-client.exe_ to prevent the user from starting while we're updating
    Archive.read(updateArchive).install();

    Path oldConfigFile = updateArchive.resolveSibling("update4j-old.xml");
    if (Files.exists(oldConfigFile)) {
      Configuration oldConfig = readConfiguration(oldConfigFile);
      Configuration newConfig = readConfiguration(updateArchive.resolveSibling("update4j-new.xml"));
      newConfig.deleteOldFiles(oldConfig);
    }

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
