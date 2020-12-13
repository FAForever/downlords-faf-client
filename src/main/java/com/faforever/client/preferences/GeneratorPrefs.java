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
  private final IntegerProperty waterLow;
  private final IntegerProperty waterHigh;
  private final BooleanProperty waterRandom;
  private final IntegerProperty plateauLow;
  private final IntegerProperty plateauHigh;
  private final BooleanProperty plateauRandom;
  private final IntegerProperty mountainLow;
  private final IntegerProperty mountainHigh;
  private final BooleanProperty mountainRandom;
  private final IntegerProperty rampLow;
  private final IntegerProperty rampHigh;
  private final BooleanProperty rampRandom;

  public GeneratorPrefs() {
    spawnCount = new SimpleIntegerProperty(6);
    mapSize = new SimpleStringProperty("10km");
    generationType = new SimpleObjectProperty<>(GenerationType.CASUAL);
    waterLow = new SimpleIntegerProperty(0);
    waterHigh = new SimpleIntegerProperty(127);
    waterRandom = new SimpleBooleanProperty(true);
    plateauLow = new SimpleIntegerProperty(0);
    plateauHigh = new SimpleIntegerProperty(127);
    plateauRandom = new SimpleBooleanProperty(true);
    mountainLow = new SimpleIntegerProperty(0);
    mountainHigh = new SimpleIntegerProperty(127);
    mountainRandom = new SimpleBooleanProperty(true);
    rampLow = new SimpleIntegerProperty(0);
    rampHigh = new SimpleIntegerProperty(127);
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

  public int getWaterLow() {
    return waterLow.get();
  }

  public void setWaterLow(int waterLow) {
    this.waterLow.set(waterLow);
  }

  public IntegerProperty waterLowProperty() {
    return waterLow;
  }

  public int getWaterHigh() {
    return waterHigh.get();
  }

  public void setWaterHigh(int waterHigh) {
    this.waterHigh.set(waterHigh);
  }

  public IntegerProperty waterHighProperty() {
    return waterHigh;
  }

  public int getPlateauLow() {
    return plateauLow.get();
  }

  public void setPlateauLow(int plateauLow) {
    this.plateauLow.set(plateauLow);
  }

  public IntegerProperty plateauLowProperty() {
    return plateauLow;
  }

  public int getPlateauHigh() {
    return plateauHigh.get();
  }

  public void setPlateauHigh(int plateauHigh) {
    this.plateauHigh.set(plateauHigh);
  }

  public IntegerProperty plateauHighProperty() {
    return plateauHigh;
  }

  public int getMountainLow() {
    return mountainLow.get();
  }

  public void setMountainLow(int mountainLow) {
    this.mountainLow.set(mountainLow);
  }

  public IntegerProperty mountainLowProperty() {
    return mountainLow;
  }

  public int getMountainHigh() {
    return mountainHigh.get();
  }

  public void setMountainHigh(int mountainHigh) {
    this.mountainHigh.set(mountainHigh);
  }

  public IntegerProperty mountainHighProperty() {
    return mountainHigh;
  }

  public int getRampLow() {
    return rampLow.get();
  }

  public void setRampLow(int rampLow) {
    this.rampLow.set(rampLow);
  }

  public IntegerProperty rampLowProperty() {
    return rampLow;
  }

  public int getRampHigh() {
    return rampHigh.get();
  }

  public void setRampHigh(int rampHigh) {
    this.rampHigh.set(rampHigh);
  }

  public IntegerProperty rampHighProperty() {
    return rampHigh;
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
