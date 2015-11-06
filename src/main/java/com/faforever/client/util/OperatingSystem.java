package com.faforever.client.util;

public enum OperatingSystem {
  WINDOWS,
  MAC,
  OTHER;

  public static OperatingSystem current() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("windows")) {
      return WINDOWS;
    }
    if (osName.contains("mac")) {
      return MAC;
    }

    return OTHER;
  }
}
