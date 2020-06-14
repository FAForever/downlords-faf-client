package com.faforever.client.preferences;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class GeneratorPrefs {
  private final IntegerProperty spawnCountProperty;

  public GeneratorPrefs() {
    spawnCountProperty = new SimpleIntegerProperty(6);
  }

  public int getSpawnCountProperty() {
    return spawnCountProperty.get();
  }

  public void setSpawnCountProperty(int spawnCountProperty) {
    this.spawnCountProperty.set(spawnCountProperty);
  }

  public IntegerProperty spawnCountPropertyProperty() {
    return spawnCountProperty;
  }
}
