package com.faforever.client.util;

public final class ThemeUtil {

  private ThemeUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static String themeFile(String theme, String relativeFile) {
    return String.format("/themes/%s/%s", theme, relativeFile);
  }
}
