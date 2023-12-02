package com.faforever.client.preferences;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum TimeInfo {
  AUTO("settings.time.system", null),
  MILITARY_TIME("settings.time.24", Locale.of("de", "DE")),
  UK_TIME("settings.time.12", Locale.of("en", "UK"));

  private final String displayNameKey;
  private final Locale usedLocale;
}

