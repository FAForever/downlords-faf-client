package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mod {

  @Key
  private String id;
  @Key
  private String author;
  @Key
  private String name;
  @Key
  private String description;
  @Key
  private int downloads;
  @Key
  private int likes;
  @Key("download_url")
  private String downloadUrl;
  @Key("thumbnail_url")
  private String thumbnailUrl;
  @Key("is_ranked")
  private boolean isRanked;
  @Key("is_ui")
  private boolean isUi;
  @Key
  private String version;
  @Key("create_time")
  private String createTime;

  public Mod() {
  }

  public Mod(String id, String name, String description, String author, LocalDateTime createTime) {
    this.id = id;
    this.name = name;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public boolean isUi() {
    return isUi;
  }

  public void setUi(boolean ui) {
    isUi = ui;
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
}
