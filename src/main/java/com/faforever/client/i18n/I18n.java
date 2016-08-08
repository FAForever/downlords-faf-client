package com.faforever.client.i18n;

import java.util.Locale;

public interface I18n {

  String get(String key, Object... args);

  Locale getLocale();
}
