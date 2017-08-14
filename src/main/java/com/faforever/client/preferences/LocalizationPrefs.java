package com.faforever.client.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


public class LocalizationPrefs {
  private final ObjectProperty<LanguageInfo> language;

  public LocalizationPrefs() {
    language = new SimpleObjectProperty<>(LanguageInfo.AUTO);
  }

  public LanguageInfo getLanguage() {
    return language.get();
  }

  public void setLanguage(LanguageInfo language) {
    this.language.set(language);
  }
}
