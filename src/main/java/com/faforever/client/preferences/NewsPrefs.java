package com.faforever.client.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
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
