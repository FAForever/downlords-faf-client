package com.faforever.client.util;

public class ThemeUtil {

  public static String themeFile(String theme, String relativeFile) {
    return String.format("/themes/%s/%s", theme, relativeFile);
  }
}
