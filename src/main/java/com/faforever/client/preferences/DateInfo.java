package com.faforever.client.preferences;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum DateInfo {
  AUTO("settings.date.system", null),
  DAY_MONTH_YEAR("settings.date.dmy", Locale.of("fr", "FR")),
  MONTH_DAY_YEAR("settings.date.mdy", Locale.of("en", "US"));

  private final String displayNameKey;
  private final Locale usedLocale;
}

