package com.faforever.client.os;

import java.util.Scanner;

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
}
