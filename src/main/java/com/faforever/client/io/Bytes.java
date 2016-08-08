package com.faforever.client.io;

import java.util.Locale;

public final class Bytes {

  private Bytes() {

  }

  public static String formatSize(long value, Locale locale) {
    if (value < 1024) {
      return value + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(value)) / 10;
    return String.format(locale, "%.1f %siB", (double) value / (1L << (z * 10)), " KMGTPE".charAt(z));
  }
}
