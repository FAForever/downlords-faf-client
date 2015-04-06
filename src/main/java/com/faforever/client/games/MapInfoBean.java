package com.faforever.client.games;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MapInfoBean {

  public MapInfoBean(String name) {
    this.name = new SimpleStringProperty(name);
  }

  private StringProperty name;

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }
}
