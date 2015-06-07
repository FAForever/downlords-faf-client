package com.faforever.client.game;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MapInfoBean {

  public MapInfoBean() {
    this(null);
  }

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
