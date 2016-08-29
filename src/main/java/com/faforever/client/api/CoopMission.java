package com.faforever.client.api;

import com.google.api.client.util.Key;

public class CoopMission {
  @Key
  private String id;
  @Key
  private String name;
  @Key
  private int version;
  @Key
  private String category;
  @Key("thumbnail_url_small")
  private String thumbnailUrlSmall;
  @Key("thumbnail_url_large")
  private String thumbnailUrlLarge;
  @Key
  private String description;
  @Key("download_url")
  private String downloadUrl;
  @Key("folder_name")
  private String folderName;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getThumbnailUrlSmall() {
    return thumbnailUrlSmall;
  }

  public void setThumbnailUrlSmall(String thumbnailUrlSmall) {
    this.thumbnailUrlSmall = thumbnailUrlSmall;
  }

  public String getThumbnailUrlLarge() {
    return thumbnailUrlLarge;
  }

  public void setThumbnailUrlLarge(String thumbnailUrlLarge) {
    this.thumbnailUrlLarge = thumbnailUrlLarge;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }
}
