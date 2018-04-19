package com.faforever.client.i18n;

import javafx.collections.ObservableList;

import java.util.Locale;

public interface I18n {

  String get(String key, Object... args);

  String get(Locale locale, String key, Object... args);

  String getQuantized(String singularKey, String pluralKey, long arg);

  Locale getUserSpecificLocale();

  String number(int number);

  String numberWithSign(int number);

  String number(double number);

  String rounded(double number, int digits);

  ObservableList<Locale> getAvailableLanguages();
}
