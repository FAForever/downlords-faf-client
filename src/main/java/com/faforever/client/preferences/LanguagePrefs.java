package com.faforever.client.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.annotation.Resource;
import java.util.Locale;

public class LanguagePrefs {
  private StringProperty language;


  public LanguagePrefs() {

    language = new SimpleStringProperty(null);
    //
  }

  public String getLanguage() {
    return language.get();
  }

  public void setLanguage(String newLanguage) {
    this.language.set(newLanguage);
  }
  public StringProperty language() {
    return language;
  }


}
