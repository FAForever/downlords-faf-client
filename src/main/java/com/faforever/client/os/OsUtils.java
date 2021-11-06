package com.faforever.client.os;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public final class OsUtils {

  private OsUtils() {
    throw new AssertionError("Not instantiable");
  }

  public static String execAndGetOutput(String... cmd) throws IOException {
    Scanner scanner = new Scanner(
        Runtime.getRuntime().exec(cmd).getInputStream(), UTF_8.name()
    ).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next().trim() : "";
  }

  public static void gobbleLines(InputStream stream, Consumer<String> lineConsumer) {
    Thread thread = new Thread(() -> {
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          lineConsumer.accept(line);
        }
      } catch (IOException e) {
        throw new RuntimeException("Could not open input stream reader", e);
      }
    });
    thread.setDaemon(true);
    thread.start();
  }
}
