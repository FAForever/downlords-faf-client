package com.faforever.client.preferences;

import ch.qos.logback.classic.Level;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DeveloperPrefs {
  StringProperty gameRepositoryUrl = new SimpleStringProperty("https://github.com/FAForever/fa.git");
  StringProperty logLevel = new SimpleStringProperty(Level.DEBUG.toString());

  public String getGameRepositoryUrl() {
    return gameRepositoryUrl.get();
  }

  public void setGameRepositoryUrl(String gameRepositoryUrl) {
    this.gameRepositoryUrl.set(gameRepositoryUrl);
  }

  public StringProperty gameRepositoryUrlProperty() {
    return gameRepositoryUrl;
  }

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
