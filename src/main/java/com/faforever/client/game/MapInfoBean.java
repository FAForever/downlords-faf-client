package com.faforever.client.game;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MapInfoBean {

  private final SimpleFloatProperty rating;
  private final StringProperty description;
  private final StringProperty name;
  private final IntegerProperty plays;
  private final IntegerProperty downloads;

  public MapInfoBean() {
    this(null);
  }

  public MapInfoBean(String name) {
    this.name = new SimpleStringProperty(name);
    this.description = new SimpleStringProperty();
    this.plays = new SimpleIntegerProperty();
    this.downloads = new SimpleIntegerProperty();
    this.rating = new SimpleFloatProperty();
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
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
}
