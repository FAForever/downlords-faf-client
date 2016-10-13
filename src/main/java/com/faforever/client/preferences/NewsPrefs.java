package com.faforever.client.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NewsPrefs {
  private final StringProperty lastReadNewsUrl;

  public NewsPrefs() {
    lastReadNewsUrl = new SimpleStringProperty();
  }

  public String getLastReadNewsUrl() {
    return lastReadNewsUrl.get();
  }

  public void setLastReadNewsUrl(String lastReadNewsUrl) {
    this.lastReadNewsUrl.set(lastReadNewsUrl);
  }

  public StringProperty lastReadNewsUrlProperty() {
    return lastReadNewsUrl;
  }
}
