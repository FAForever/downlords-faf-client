package com.faforever.client.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DeveloperPrefs {
  private final StringProperty gameRepositoryUrl;

  public DeveloperPrefs() {
    gameRepositoryUrl = new SimpleStringProperty("https://github.com/FAForever/fa.git");
  }

  public String getGameRepositoryUrl() {
    return gameRepositoryUrl.get();
  }

  public void setGameRepositoryUrl(String gameRepositoryUrl) {
    this.gameRepositoryUrl.set(gameRepositoryUrl);
  }

  public StringProperty gameRepositoryUrlProperty() {
    return gameRepositoryUrl;
  }
}
