package com.faforever.client.theme;

import java.util.Objects;
import java.util.Properties;

public record Theme(String displayName, String author, Integer compatibilityVersion, String themeVersion) {

  private static final String DISPLAY_NAME = "displayName";
  private static final String AUTHOR = "author";
  private static final String COMPATIBILITY_VERSION = "compatibilityVersion";
  private static final String THEME_VERSION = "themeVersion";

  public static Theme fromProperties(Properties properties) {
    return new Theme(properties.getProperty(DISPLAY_NAME), properties.getProperty(AUTHOR),
                     Integer.valueOf(properties.getProperty(COMPATIBILITY_VERSION)),
                     properties.getProperty(THEME_VERSION));
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, themeVersion);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Theme theme = (Theme) o;
    return Objects.equals(displayName, theme.displayName) && Objects.equals(themeVersion, theme.themeVersion);
  }
}
