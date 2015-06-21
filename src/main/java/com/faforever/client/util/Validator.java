package com.faforever.client.util;

import java.util.regex.Pattern;

public final class Validator {

  private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

  private Validator() {
    // Utility class
  }

  public static boolean isInt(String string) {
    return INT_PATTERN.matcher(string).matches();
  }

}
