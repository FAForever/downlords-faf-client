package com.faforever.client.os;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.function.Consumer;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class OsUtils {

  private OsUtils() {
    throw new AssertionError("Not instantiable");
  }

  public static String execAndGetOutput(String cmd) {
    Scanner scanner = new Scanner(
        noCatch(() -> Runtime.getRuntime().exec(cmd)).getInputStream(), UTF_8.name()
    ).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next().trim() : "";
  }

  public static void gobbleLines(InputStream stream, Consumer<String> lineConsumer) {
    Thread thread = new Thread(() -> noCatch(() -> {
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream))) {
        lineConsumer.accept(bufferedReader.readLine());
      }
    }));
    thread.setDaemon(true);
    thread.start();
  }
}
