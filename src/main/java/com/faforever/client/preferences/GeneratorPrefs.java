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
  private final StringProperty commandLineArgs;
  private final IntegerProperty spawnCount;
  private final IntegerProperty numTeams;
  private final StringProperty mapSize;
  private final StringProperty mapStyle;
  private final IntegerProperty waterDensity;
  private final BooleanProperty waterRandom;
  private final IntegerProperty plateauDensity;
  private final BooleanProperty plateauRandom;
  private final IntegerProperty mountainDensity;
  private final BooleanProperty mountainRandom;
  private final IntegerProperty rampDensity;
  private final BooleanProperty rampRandom;
  private final IntegerProperty mexDensity;
  private final BooleanProperty mexRandom;
  private final IntegerProperty reclaimDensity;
  private final BooleanProperty reclaimRandom;

  public GeneratorPrefs() {
    commandLineArgs = new SimpleStringProperty("");
    spawnCount = new SimpleIntegerProperty(6);
    numTeams = new SimpleIntegerProperty(2);
    mapSize = new SimpleStringProperty("10km");
    mapStyle = new SimpleStringProperty("");
    generationType = new SimpleObjectProperty<>(GenerationType.CASUAL);
    waterDensity = new SimpleIntegerProperty(0);
    waterRandom = new SimpleBooleanProperty(true);
    plateauDensity = new SimpleIntegerProperty(0);
    plateauRandom = new SimpleBooleanProperty(true);
    mountainDensity = new SimpleIntegerProperty(0);
    mountainRandom = new SimpleBooleanProperty(true);
    rampDensity = new SimpleIntegerProperty(0);
    rampRandom = new SimpleBooleanProperty(true);
    mexDensity = new SimpleIntegerProperty(0);
    mexRandom = new SimpleBooleanProperty(true);
    reclaimDensity = new SimpleIntegerProperty(0);
    reclaimRandom = new SimpleBooleanProperty(true);
  }

  public String getCommandLineArgs() {
    return commandLineArgs.get();
  }

  public void setCommandLineArgs(String commandLineArgs) {
    this.commandLineArgs.set(commandLineArgs);
  }

  public StringProperty commandLineArgsProperty() {
    return commandLineArgs;
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

  public int getNumTeams() {
    return numTeams.get();
  }

  public void setNumTeams(int numTeams) {
    this.numTeams.set(numTeams);
  }

  public IntegerProperty numTeamsProperty() {
    return numTeams;
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

  public String getMapStyle() {
    return mapStyle.get();
  }

  public void setMapStyle(String mapStyle) {
    this.mapStyle.set(mapStyle);
  }

  public StringProperty mapStyleProperty() {
    return mapStyle;
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

  public int getMexDensity() {
    return mexDensity.get();
  }

  public void setMexDensity(int mexDensity) {
    this.mexDensity.set(mexDensity);
  }

  public IntegerProperty mexDensityProperty() {
    return mexDensity;
  }

  public int getReclaimDensity() {
    return reclaimDensity.get();
  }

  public void setReclaimDensity(int reclaimDensity) {
    this.reclaimDensity.set(reclaimDensity);
  }

  public IntegerProperty reclaimDensityProperty() {
    return reclaimDensity;
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

  public boolean getMexRandom() {
    return mexRandom.get();
  }

  public void setMexRandom(boolean mexRandom) {
    this.mexRandom.set(mexRandom);
  }

  public BooleanProperty mexRandomProperty() {
    return mexRandom;
  }

  public boolean getReclaimRandom() {
    return reclaimRandom.get();
  }

  public void setReclaimRandom(boolean reclaimRandom) {
    this.reclaimRandom.set(reclaimRandom);
  }

  public BooleanProperty reclaimRandomProperty() {
    return reclaimRandom;
  }
}
