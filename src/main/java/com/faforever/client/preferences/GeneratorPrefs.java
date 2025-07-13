package com.faforever.client.preferences;

import com.faforever.client.map.generator.GenerationType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class GeneratorPrefs {
  private final ObjectProperty<GenerationType> generationType = new SimpleObjectProperty<>(GenerationType.CASUAL);
  private final StringProperty commandLineArgs = new SimpleStringProperty("");
  private final IntegerProperty spawnCount = new SimpleIntegerProperty(6);
  private final IntegerProperty numTeams = new SimpleIntegerProperty(2);
  private final DoubleProperty mapSizeInKm = new SimpleDoubleProperty(10);
  private final ObservableList<String> mapStyles = FXCollections.observableArrayList();
  private final ObservableList<String> symmetry = FXCollections.observableArrayList();
  private final StringProperty seed = new SimpleStringProperty("");
  private final BooleanProperty fixedSeed = new SimpleBooleanProperty(false);
  private final BooleanProperty customStyle = new SimpleBooleanProperty(false);
  private final ObservableList<String> terrainStyles = FXCollections.observableArrayList();
  private final ObservableList<String> textureStyles = FXCollections.observableArrayList();
  private final ObservableList<String> resourceStyles = FXCollections.observableArrayList();
  private final ObservableList<String> propStyles = FXCollections.observableArrayList();
  private final IntegerProperty reclaimDensityMin = new SimpleIntegerProperty(0);
  private final IntegerProperty reclaimDensityMax = new SimpleIntegerProperty(127);
  private final IntegerProperty resourceDensityMin = new SimpleIntegerProperty(0);
  private final IntegerProperty resourceDensityMax = new SimpleIntegerProperty(127);

  public GenerationType getGenerationType() {
    return generationType.get();
  }

  public void setGenerationType(GenerationType generationType) {
    this.generationType.set(generationType);
  }

  public ObjectProperty<GenerationType> generationTypeProperty() {
    return generationType;
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

  public double getMapSizeInKm() {
    return mapSizeInKm.get();
  }

  public void setMapSizeInKm(Double mapSizeInKm) {
    this.mapSizeInKm.set(mapSizeInKm);
  }

  public DoubleProperty mapSizeInKmProperty() {
    return mapSizeInKm;
  }

  public ObservableList<String> getMapStyles() {
    return mapStyles;
  }

  public ObservableList<String> getSymmetries() {
    return symmetry;
  }

  public String getSeed() {
    return seed.get();
  }

  public void setSeed(String seed) {
    this.seed.set(seed);
  }

  public StringProperty seedProperty() {
    return seed;
  }

  public boolean getFixedSeed() {
    return fixedSeed.get();
  }

  public void setFixedSeed(boolean fixedSeed) {
    this.fixedSeed.set(fixedSeed);
  }

  public BooleanProperty fixedSeedProperty() {
    return fixedSeed;
  }

  public boolean getCustomStyle() {
    return customStyle.get();
  }

  public void setCustomStyle(boolean customStyle) {
    this.customStyle.set(customStyle);
  }

  public BooleanProperty customStyleProperty() {
    return customStyle;
  }

  public ObservableList<String> getTerrainStyles() {
    return terrainStyles;
  }

  public ObservableList<String> getTextureStyles() {
    return textureStyles;
  }

  public ObservableList<String> getResourceStyles() {
    return resourceStyles;
  }

  public ObservableList<String> getPropStyles() {
    return propStyles;
  }

  public boolean isFixedSeed() {
    return fixedSeed.get();
  }

  public boolean isCustomStyle() {
    return customStyle.get();
  }

  public int getReclaimDensityMin() {
    return reclaimDensityMin.get();
  }

  public IntegerProperty reclaimDensityMinProperty() {
    return reclaimDensityMin;
  }

  public void setReclaimDensityMin(int min) {
    this.reclaimDensityMin.set(min);
  }

  public int getReclaimDensityMax() {
    return reclaimDensityMax.get();
  }

  public IntegerProperty reclaimDensityMaxProperty() {
    return reclaimDensityMax;
  }

  public void setReclaimDensityMax(int max) {
    this.reclaimDensityMax.set(max);
  }

  public int getResourceDensityMin() {
    return resourceDensityMin.get();
  }

  public IntegerProperty resourceDensityMinProperty() {
    return resourceDensityMin;
  }

  public void setResourceDensityMin(int min) {
    this.resourceDensityMin.set(min);
  }

  public int getResourceDensityMax() {
    return resourceDensityMax.get();
  }

  public IntegerProperty resourceDensityMaxProperty() {
    return resourceDensityMax;
  }

  public void setResourceDensityMax(int max) {
    this.resourceDensityMax.set(max);
  }
}
