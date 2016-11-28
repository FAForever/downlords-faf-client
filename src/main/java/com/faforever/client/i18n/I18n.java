package com.faforever.client.i18n;

import java.util.Locale;

public interface I18n {

  String get(String key, Object... args);

  String getQuantized(String singularKey, String pluralKey, long arg);

  Locale getLocale();

  String number(int number);
}
