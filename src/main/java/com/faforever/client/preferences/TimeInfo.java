package com.faforever.client.preferences;

import java.util.Locale;

public enum TimeInfo {
  AUTO("settings.time.system", null),
  MILITARY_TIME("settings.time.24", new Locale("de", "DE")),
  UK_TIME("settings.time.12", new Locale("en", "UK"));

  private final Locale usedLocale;
  private final String displayNameKey;

  TimeInfo(String displayNameKey, Locale usedLocale) {
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

