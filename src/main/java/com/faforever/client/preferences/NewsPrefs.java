package com.faforever.client.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Value;

@Value
public class NewsPrefs {
  StringProperty lastReadNewsUrl = new SimpleStringProperty();

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
