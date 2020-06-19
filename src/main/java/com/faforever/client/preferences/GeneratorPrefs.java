package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class GeneratorPrefs {
  private final IntegerProperty spawnCountProperty;
  private final BooleanProperty generateWaterProperty;

  public GeneratorPrefs() {
    spawnCountProperty = new SimpleIntegerProperty(6);
    generateWaterProperty = new SimpleBooleanProperty(false);
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

  public boolean getGenerateWaterProperty() {
    return generateWaterProperty.get();
  }

  public void setGenerateWaterProperty(boolean generateWaterProperty) {
    this.generateWaterProperty.set(generateWaterProperty);
  }

  public BooleanProperty generateWaterPropertyProperty() {
    return generateWaterProperty;
  }
}
