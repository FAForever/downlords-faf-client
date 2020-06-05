package com.faforever.client.bootstrap;

import org.update4j.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Bootstrap {
  public static void main(String[] args) throws IOException {
    Configuration config;

    try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get("build/update4j/config.xml"))) {
      config = Configuration.read(bufferedReader);
    }

    config.update();
    config.launch();
  }
}
