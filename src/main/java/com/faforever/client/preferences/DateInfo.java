package com.faforever.client.preferences;

import java.util.Locale;

public enum DateInfo {
  AUTO("settings.date.system", null),
  DAY_MONTH_YEAR("settings.date.dmy", new Locale("fr", "FR")),
  MONTH_DAY_YEAR("settings.date.mdy", new Locale("en", "US"));

  private final Locale usedLocale;
  private final String displayNameKey;

  DateInfo(String displayNameKey, Locale usedLocale) {
    this.displayNameKey = displayNameKey;
    this.usedLocale = usedLocale;
  }

  public Locale getUsedLocale() {
    return usedLocale;
  }

  public String getDisplayNameKey() {
    return displayNameKey;
  }
}

