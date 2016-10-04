package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mod {

  @Key
  private String id;
  @Key
  private String author;
  @Key("display_name")
  private String displayName;
  @Key
  private String description;
  @Key
  private int downloads;
  @Key
  private int likes;
  @Key("times_played")
  private int timesPlayed;
  @Key("download_url")
  private String downloadUrl;
  @Key("thumbnail_url")
  private String thumbnailUrl;
  @Key("is_ranked")
  private boolean isRanked;
  @Key("type")
  private String type;
  @Key
  private String version;
  @Key("create_time")
  private String createTime;

  public Mod() {
  }

  public Mod(String id, String displayName, String description, String author, LocalDateTime createTime) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.author = author;
    setCreateTime(createTime);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  public int getLikes() {
    return likes;
  }

  public void setLikes(int likes) {
    this.likes = likes;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public boolean isRanked() {
    return isRanked;
  }

  public void setRanked(boolean ranked) {
    isRanked = ranked;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public LocalDateTime getCreateTime() {
    return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(createTime));
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = DateTimeFormatter.ISO_DATE_TIME.format(createTime);
  }

  public int getTimesPlayed() {
    return timesPlayed;
  }

  public void setTimesPlayed(int timesPlayed) {
    this.timesPlayed = timesPlayed;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
