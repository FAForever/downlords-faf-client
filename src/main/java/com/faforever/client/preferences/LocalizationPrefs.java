package com.faforever.client.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public class LocalizationPrefs {
  private final ObjectProperty<Locale> language;
  private final ObjectProperty<DateInfo> dateFormat;

  public LocalizationPrefs() {
    language = new SimpleObjectProperty<>();
    dateFormat = new SimpleObjectProperty<>(DateInfo.AUTO);
  }

  @Nullable
  public Locale getLanguage() {
    return language.get();
  }

  public void setLanguage(Locale language) {
    this.language.set(language);
  }

  public ObjectProperty<Locale> languageProperty() {
    return language;
  }

  public DateInfo getDateFormat() {
    return dateFormat.get();
  }

  public void setDateFormat(DateInfo date) {
    this.dateFormat.set(date);
  }
}
