package com.faforever.client.preferences;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class GeneratorPrefs {
  private final IntegerProperty spawnCountProperty;
  private final IntegerProperty landDensityProperty;

  public GeneratorPrefs() {
    spawnCountProperty = new SimpleIntegerProperty(6);
    landDensityProperty = new SimpleIntegerProperty(51);
  }

  public int getSpawnCountProperty() {
    return spawnCountProperty.get();
  }

  public void setSpawnCountProperty(int spawnCountProperty) {
    this.spawnCountProperty.set(spawnCountProperty);
  }

  public int getLandDensityProperty() {
    return landDensityProperty.get();
  }

  public void setLandDensityProperty(int landDensityProperty) {
    this.landDensityProperty.set(landDensityProperty);
  }

  public IntegerProperty spawnCountPropertyProperty() {
    return spawnCountProperty;
  }

  public IntegerProperty landDensityPropertyProperty() {
    return landDensityProperty;
  }
}
