package com.faforever.client.util;

import org.apache.commons.lang3.StringUtils;

public final class AchievementUtil {

  private AchievementUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static String defaultIcon(String theme, String iconUrl) {
    if (StringUtils.isEmpty(iconUrl)) {
      return ThemeUtil.themeFile(theme, "images/default_achievement.png");
    }
    return iconUrl;
  }
}
