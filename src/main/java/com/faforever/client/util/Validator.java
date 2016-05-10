package com.faforever.client.util;

import java.util.regex.Pattern;

public final class Validator {

  private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

  private Validator() {
    // Utility class
  }

  /**
   * Throws a NullPointerException with the specified message when {@code object} is null.
   *
   * @param object the object to check for null
   * @param message the exception message
   */
  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
  }

  public static boolean isInt(String string) {
    return INT_PATTERN.matcher(string).matches();
  }

}
