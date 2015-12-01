package com.faforever.client.api;

import com.google.api.client.util.Key;

public class Map {

  @Key("map_type")
  String mapType;
  @Key("max_players")
  int maxPlayers;
  @Key
  private String id;
  @Key("battle_type")
  private String battletype;
  @Key
  private String description;
  @Key
  private int downloads;
  @Key
  private String filename;
  @Key("display_name")
  private String displayName;
  @Key("technical_name")
  private String technicalName;
  @Key("num_draws")
  private int numDraws;
  @Key
  private float rating;
  @Key("times_played")
  private int timesPlayed;
  @Key
  private String version;
  @Key("map_size_x")
  private int sizeX;
  @Key("map_size_y")
  private int sizeY;

  public int getSizeX() {
    return sizeX;
  }

  public void setSizeX(int sizeX) {
    this.sizeX = sizeX;
  }

  public int getSizeY() {
    return sizeY;
  }

  public void setSizeY(int sizeY) {
    this.sizeY = sizeY;
  }

  public String getTechnicalName() {
    return technicalName;
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName = technicalName;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBattletype() {
    return battletype;
  }

  public void setBattletype(String battletype) {
    this.battletype = battletype;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getDownloads() {
    return downloads;
  }

  public void setDownloads(int downloads) {
    this.downloads = downloads;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getMapType() {
    return mapType;
  }

  public void setMapType(String mapType) {
    this.mapType = mapType;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getNumDraws() {
    return numDraws;
  }

  public void setNumDraws(int numDraws) {
    this.numDraws = numDraws;
  }

  public float getRating() {
    return rating;
  }

  public void setRating(float rating) {
    this.rating = rating;
  }

  public int getTimesPlayed() {
    return timesPlayed;
  }

  public void setTimesPlayed(int timesPlayed) {
    this.timesPlayed = timesPlayed;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
