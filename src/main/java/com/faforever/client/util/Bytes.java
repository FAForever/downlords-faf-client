package com.faforever.client.util;

public final class Bytes {

  private Bytes() {

  }

  public static String formatSize(long value) {
    if (value < 1024) {
      return value + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(value)) / 10;
    return String.format("%.1f %siB", (double) value / (1L << (z * 10)), " KMGTPE".charAt(z));
  }
}
