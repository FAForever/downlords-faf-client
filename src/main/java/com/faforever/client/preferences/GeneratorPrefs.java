package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GeneratorPrefs {
  private final IntegerProperty spawnCountProperty;
  private final StringProperty mapSizeProperty;
  private final IntegerProperty waterDensityProperty;
  private final BooleanProperty waterRandomProperty;
  private final IntegerProperty plateauDensityProperty;
  private final BooleanProperty plateauRandomProperty;
  private final IntegerProperty mountainDensityProperty;
  private final BooleanProperty mountainRandomProperty;
  private final IntegerProperty rampDensityProperty;
  private final BooleanProperty rampRandomProperty;

  public GeneratorPrefs() {
    spawnCountProperty = new SimpleIntegerProperty(6);
    mapSizeProperty = new SimpleStringProperty("10km");
    waterDensityProperty = new SimpleIntegerProperty(0);
    waterRandomProperty = new SimpleBooleanProperty(true);
    plateauDensityProperty = new SimpleIntegerProperty(0);
    plateauRandomProperty = new SimpleBooleanProperty(true);
    mountainDensityProperty = new SimpleIntegerProperty(0);
    mountainRandomProperty = new SimpleBooleanProperty(true);
    rampDensityProperty = new SimpleIntegerProperty(0);
    rampRandomProperty = new SimpleBooleanProperty(true);
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

  public String getMapSizeProperty() {
    return mapSizeProperty.get();
  }

  public void setMapSizeProperty(String mapSizeProperty) {
    this.mapSizeProperty.set(mapSizeProperty);
  }

  public StringProperty mapSizePropertyProperty() {
    return mapSizeProperty;
  }

  public int getWaterDensityProperty() {
    return waterDensityProperty.get();
  }

  public void setWaterDensityProperty(int waterDensityProperty) {
    this.waterDensityProperty.set(waterDensityProperty);
  }

  public IntegerProperty waterDensityPropertyProperty() {
    return waterDensityProperty;
  }

  public int getPlateauDensityProperty() {
    return plateauDensityProperty.get();
  }

  public void setPlateauDensityProperty(int plateauDensityProperty) {
    this.plateauDensityProperty.set(plateauDensityProperty);
  }

  public IntegerProperty plateauDensityPropertyProperty() {
    return plateauDensityProperty;
  }

  public int getMountainDensityProperty() {
    return mountainDensityProperty.get();
  }

  public void setMountainDensityProperty(int mountainDensityProperty) {
    this.mountainDensityProperty.set(mountainDensityProperty);
  }

  public IntegerProperty mountainDensityPropertyProperty() {
    return mountainDensityProperty;
  }

  public int getRampDensityProperty() {
    return rampDensityProperty.get();
  }

  public void setRampDensityProperty(int rampDensityProperty) {
    this.rampDensityProperty.set(rampDensityProperty);
  }

  public IntegerProperty rampDensityPropertyProperty() {
    return rampDensityProperty;
  }

  public boolean getWaterRandomProperty() {
    return waterRandomProperty.get();
  }

  public void setWaterRandomProperty(boolean waterRandomProperty) {
    this.waterRandomProperty.set(waterRandomProperty);
  }

  public BooleanProperty waterRandomPropertyProperty() {
    return waterRandomProperty;
  }

  public boolean getMountainRandomProperty() {
    return mountainRandomProperty.get();
  }

  public void setMountainRandomProperty(boolean mountainRandomProperty) {
    this.mountainRandomProperty.set(mountainRandomProperty);
  }

  public BooleanProperty mountainRandomPropertyProperty() {
    return mountainRandomProperty;
  }

  public boolean getPlateauRandomProperty() {
    return plateauRandomProperty.get();
  }

  public void setPlateauRandomProperty(boolean plateauRandomProperty) {
    this.plateauRandomProperty.set(plateauRandomProperty);
  }

  public BooleanProperty plateauRandomPropertyProperty() {
    return plateauRandomProperty;
  }

  public boolean getRampRandomProperty() {
    return rampRandomProperty.get();
  }

  public void setRampRandomProperty(boolean rampRandomProperty) {
    this.rampRandomProperty.set(rampRandomProperty);
  }

  public BooleanProperty rampRandomPropertyProperty() {
    return rampRandomProperty;
  }
}
