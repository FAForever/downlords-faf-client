package com.faforever.client.preferences;

import com.faforever.client.map.generator.GenerationType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GeneratorPrefs {
  private final ObjectProperty<GenerationType> generationType;
  private final IntegerProperty spawnCount;
  private final StringProperty mapSize;
  private final IntegerProperty waterDensity;
  private final BooleanProperty waterRandom;
  private final IntegerProperty plateauDensity;
  private final BooleanProperty plateauRandom;
  private final IntegerProperty mountainDensity;
  private final BooleanProperty mountainRandom;
  private final IntegerProperty rampDensity;
  private final BooleanProperty rampRandom;

  public GeneratorPrefs() {
    spawnCount = new SimpleIntegerProperty(6);
    mapSize = new SimpleStringProperty("10km");
    generationType = new SimpleObjectProperty<>(GenerationType.CASUAL);
    waterDensity = new SimpleIntegerProperty(0);
    waterRandom = new SimpleBooleanProperty(true);
    plateauDensity = new SimpleIntegerProperty(0);
    plateauRandom = new SimpleBooleanProperty(true);
    mountainDensity = new SimpleIntegerProperty(0);
    mountainRandom = new SimpleBooleanProperty(true);
    rampDensity = new SimpleIntegerProperty(0);
    rampRandom = new SimpleBooleanProperty(true);
  }

  public int getSpawnCount() {
    return spawnCount.get();
  }

  public void setSpawnCount(int spawnCount) {
    this.spawnCount.set(spawnCount);
  }

  public IntegerProperty spawnCountProperty() {
    return spawnCount;
  }

  public String getMapSize() {
    return mapSize.get();
  }

  public void setMapSize(String mapSize) {
    this.mapSize.set(mapSize);
  }

  public StringProperty mapSizeProperty() {
    return mapSize;
  }

  public GenerationType getGenerationType() {
    return generationType.get();
  }

  public void setGenerationType(GenerationType generationType) {
    this.generationType.set(generationType);
  }

  public ObjectProperty<GenerationType> generationTypeProperty() {
    return generationType;
  }

  public int getWaterDensity() {
    return waterDensity.get();
  }

  public void setWaterDensity(int waterDensity) {
    this.waterDensity.set(waterDensity);
  }

  public IntegerProperty waterDensityProperty() {
    return waterDensity;
  }

  public int getPlateauDensity() {
    return plateauDensity.get();
  }

  public void setPlateauDensity(int plateauDensity) {
    this.plateauDensity.set(plateauDensity);
  }

  public IntegerProperty plateauDensityProperty() {
    return plateauDensity;
  }

  public int getMountainDensity() {
    return mountainDensity.get();
  }

  public void setMountainDensity(int mountainDensity) {
    this.mountainDensity.set(mountainDensity);
  }

  public IntegerProperty mountainDensityProperty() {
    return mountainDensity;
  }

  public int getRampDensity() {
    return rampDensity.get();
  }

  public void setRampDensity(int rampDensity) {
    this.rampDensity.set(rampDensity);
  }

  public IntegerProperty rampDensityProperty() {
    return rampDensity;
  }

  public boolean getWaterRandom() {
    return waterRandom.get();
  }

  public void setWaterRandom(boolean waterRandom) {
    this.waterRandom.set(waterRandom);
  }

  public BooleanProperty waterRandomProperty() {
    return waterRandom;
  }

  public boolean getMountainRandom() {
    return mountainRandom.get();
  }

  public void setMountainRandom(boolean mountainRandom) {
    this.mountainRandom.set(mountainRandom);
  }

  public BooleanProperty mountainRandomProperty() {
    return mountainRandom;
  }

  public boolean getPlateauRandom() {
    return plateauRandom.get();
  }

  public void setPlateauRandom(boolean plateauRandom) {
    this.plateauRandom.set(plateauRandom);
  }

  public BooleanProperty plateauRandomProperty() {
    return plateauRandom;
  }

  public boolean getRampRandom() {
    return rampRandom.get();
  }

  public void setRampRandom(boolean rampRandom) {
    this.rampRandom.set(rampRandom);
  }

  public BooleanProperty rampRandomProperty() {
    return rampRandom;
  }
}
