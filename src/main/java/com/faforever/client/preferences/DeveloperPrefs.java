package com.faforever.client.preferences;

import ch.qos.logback.classic.Level;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DeveloperPrefs {
  StringProperty logLevel = new SimpleStringProperty(Level.DEBUG.toString());

  public String getLogLevel() {
    return logLevel.get();
  }

  public StringProperty logLevelProperty() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel.set(logLevel);
  }
}
