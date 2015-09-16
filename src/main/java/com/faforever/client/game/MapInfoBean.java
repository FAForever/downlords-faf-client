package com.faforever.client.game;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;

public class MapInfoBean implements Comparable<MapInfoBean> {

  private final StringProperty technicalName;
  private final StringProperty displayName;
  private final SimpleFloatProperty rating;
  private final IntegerProperty plays;
  private final StringProperty description;
  private final IntegerProperty downloads;
  private final IntegerProperty players;
  private final ObjectProperty<MapSize> size;
  private final IntegerProperty version;
  private final IntegerProperty id;
  private final BooleanProperty hasAiMarkers;

  public MapInfoBean() {
    this(null);
  }

  public MapInfoBean(String technicalName) {
    this.id = new SimpleIntegerProperty();

    // FIXME
    this.displayName = new SimpleStringProperty(technicalName);
    this.technicalName = new SimpleStringProperty(technicalName);
    this.description = new SimpleStringProperty();
    this.plays = new SimpleIntegerProperty();
    this.downloads = new SimpleIntegerProperty();
    this.rating = new SimpleFloatProperty();
    this.players = new SimpleIntegerProperty();
    this.size = new SimpleObjectProperty<>();
    this.version = new SimpleIntegerProperty();
    this.hasAiMarkers = new SimpleBooleanProperty();
  }

  public boolean getHasAiMarkers() {
    return hasAiMarkers.get();
  }

  public void setHasAiMarkers(boolean hasAiMarkers) {
    this.hasAiMarkers.set(hasAiMarkers);
  }

  public BooleanProperty hasAiMarkersProperty() {
    return hasAiMarkers;
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public int getPlays() {
    return plays.get();
  }

  public void setPlays(int plays) {
    this.plays.set(plays);
  }

  public IntegerProperty playsProperty() {
    return plays;
  }

  public int getDownloads() {
    return downloads.get();
  }

  public void setDownloads(int downloads) {
    this.downloads.set(downloads);
  }

  public IntegerProperty downloadsProperty() {
    return downloads;
  }

  public float getRating() {
    return rating.get();
  }

  public void setRating(float rating) {
    this.rating.set(rating);
  }

  public SimpleFloatProperty ratingProperty() {
    return rating;
  }

  public MapSize getSize() {
    return size.get();
  }

  public void setSize(MapSize size) {
    this.size.set(size);
  }

  public ObjectProperty<MapSize> sizeProperty() {
    return size;
  }

  public int getPlayers() {
    return players.get();
  }

  public void setPlayers(int players) {
    this.players.set(players);
  }

  public IntegerProperty playersProperty() {
    return players;
  }

  public int getVersion() {
    return version.get();
  }

  public void setVersion(int version) {
    this.version.set(version);
  }

  public IntegerProperty versionProperty() {
    return version;
  }

  @Override
  public int compareTo(@NotNull MapInfoBean o) {
    return getDisplayName().compareTo(o.getDisplayName());
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public String getTechnicalName() {
    return technicalName.get();
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName.set(technicalName);
  }

  public StringProperty technicalNameProperty() {
    return technicalName;
  }
}
