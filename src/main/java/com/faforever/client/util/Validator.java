package com.faforever.client.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class Validator {

  private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

  private Validator() {
    // Utility class
  }

  public static boolean isInt(String string) {
    return INT_PATTERN.matcher(string).matches();
  }

  public static boolean isAscii(String content) {
    return StandardCharsets.US_ASCII.newEncoder().canEncode(content);
  }
}
