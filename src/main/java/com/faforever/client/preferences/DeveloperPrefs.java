package com.faforever.client.preferences;

import ch.qos.logback.classic.Level;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class DeveloperPrefs {
  private final StringProperty logLevel = new SimpleStringProperty(Level.DEBUG.toString());

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
