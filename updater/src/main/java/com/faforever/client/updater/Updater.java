package com.faforever.client.updater;

import org.update4j.Configuration;
import org.update4j.Update;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class Updater {
  public static void main(String[] args) throws IOException {
    Preferences preferences = Preferences.userRoot().node("/com/faforever/client");
    String updateDir = preferences.get("updateDir", null);
    if (updateDir != null) {
      Update.finalizeUpdate(Paths.get(updateDir));
    }

    Configuration config;
    try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get("build/resources/main/update4j/update4j.xml"))) {
      config = Configuration.read(bufferedReader);
    }

    config.update();
    config.launch();
  }
}
