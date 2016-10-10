package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.nocatch.NoCatch.noCatch;

public class Map {

  @Key("author")
  private String author;
  @Key("battle_type")
  private String battle_type;
  @Key("create_time")
  private String createTime;
  @Key("description")
  private String description;
  @Key("display_name")
  private String displayName;
  @Key("download_url")
  private String downloadUrl;
  @Key("thumbnail_url_small")
  private String thumbnailUrlSmall;
  @Key("thumbnail_url_large")
  private String thumbnailUrlLarge;
  @Key("downloads")
  private int downloads;
  @Key("id")
  private String id;
  @Key("map_type")
  private String mapType;
  @Key("max_players")
  private int maxPlayers;
  @Key("num_draws")
  private int numDraws;
  @Key("rating")
  private Float rating;
  @Key("technical_name")
  private String technicalName;
  @Key("times_played")
  private int timesPlayed;
  @Key("version")
  private int version;
  @Key("width")
  private int sizeX;
  @Key("height")
  private int sizeY;

  public URL getThumbnailUrlSmall() {
    if (thumbnailUrlSmall == null) {
      return null;
    }
    return noCatch(() -> new URL(thumbnailUrlSmall));
  }

  public void setThumbnailUrlSmall(URL thumbnailUrlSmall) {
    this.thumbnailUrlSmall = thumbnailUrlSmall.toString();
  }

  public URL getThumbnailUrlLarge() {
    if (thumbnailUrlLarge == null) {
      return null;
    }
    return noCatch(() -> new URL(thumbnailUrlLarge));
  }

  public void setThumbnailUrlLarge(URL thumbnailUrlLarge) {
    this.thumbnailUrlLarge = thumbnailUrlLarge.toString();
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getBattle_type() {
    return battle_type;
  }

  public void setBattle_type(String battle_type) {
    this.battle_type = battle_type;
  }

  public LocalDateTime getCreateTime() {
    return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(createTime));
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = DateTimeFormatter.ISO_DATE_TIME.format(createTime);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public URL getDownloadUrl() {
    if (downloadUrl == null) {
      return null;
    }
    return noCatch(() -> new URL(downloadUrl));
  }

  public void setDownloadUrl(URL downloadUrl) {
    this.downloadUrl = downloadUrl.toString();
  }

  public int getDownloads() {
    return downloads;
  }

  public void setDownloads(int downloads) {
    this.downloads = downloads;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public int getNumDraws() {
    return numDraws;
  }

  public void setNumDraws(int numDraws) {
    this.numDraws = numDraws;
  }

  public Float getRating() {
    return rating;
  }

  public void setRating(Float rating) {
    this.rating = rating;
  }

  public String getTechnicalName() {
    return technicalName;
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName = technicalName;
  }

  public int getTimesPlayed() {
    return timesPlayed;
  }

  public void setTimesPlayed(int timesPlayed) {
    this.timesPlayed = timesPlayed;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

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
}
