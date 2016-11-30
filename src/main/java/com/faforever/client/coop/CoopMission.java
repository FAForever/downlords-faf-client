package com.faforever.client.coop;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CoopMission {

  private StringProperty id;
  private StringProperty name;
  private StringProperty description;
  private IntegerProperty version;
  private ObjectProperty<CoopCategory> category;
  private StringProperty downloadUrl;
  private StringProperty thumbnailUrlSmall;
  private StringProperty thumbnailUrlLarge;
  private StringProperty mapFolderName;

  public CoopMission() {
    id = new SimpleStringProperty();
    name = new SimpleStringProperty();
    description = new SimpleStringProperty();
    version = new SimpleIntegerProperty();
    category = new SimpleObjectProperty<>();
    downloadUrl = new SimpleStringProperty();
    thumbnailUrlSmall = new SimpleStringProperty();
    thumbnailUrlLarge = new SimpleStringProperty();
    mapFolderName = new SimpleStringProperty();
  }

  public static CoopMission fromCoopInfo(com.faforever.client.api.CoopMission mission) {
    CoopMission bean = new CoopMission();
    bean.setId(mission.getId());
    bean.setDescription(mission.getDescription());
    bean.setName(mission.getName());
    bean.setVersion(mission.getVersion());
    bean.setCategory(CoopCategory.valueOf(mission.getCategory()));
    bean.setVersion(mission.getVersion());
    bean.setDownloadUrl(mission.getDownloadUrl());
    bean.setThumbnailUrlLarge(mission.getThumbnailUrlLarge());
    bean.setThumbnailUrlSmall(mission.getThumbnailUrlSmall());
    bean.setMapFolderName(mission.getFolderName());
    return bean;
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

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
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

  public CoopCategory getCategory() {
    return category.get();
  }

  public void setCategory(CoopCategory category) {
    this.category.set(category);
  }

  public ObjectProperty<CoopCategory> categoryProperty() {
    return category;
  }

  public String getDownloadUrl() {
    return downloadUrl.get();
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl.set(downloadUrl);
  }

  public StringProperty downloadUrlProperty() {
    return downloadUrl;
  }

  public String getThumbnailUrlSmall() {
    return thumbnailUrlSmall.get();
  }

  public void setThumbnailUrlSmall(String thumbnailUrlSmall) {
    this.thumbnailUrlSmall.set(thumbnailUrlSmall);
  }

  public StringProperty thumbnailUrlSmallProperty() {
    return thumbnailUrlSmall;
  }

  public String getThumbnailUrlLarge() {
    return thumbnailUrlLarge.get();
  }

  public void setThumbnailUrlLarge(String thumbnailUrlLarge) {
    this.thumbnailUrlLarge.set(thumbnailUrlLarge);
  }

  public StringProperty thumbnailUrlLargeProperty() {
    return thumbnailUrlLarge;
  }

  public String getMapFolderName() {
    return mapFolderName.get();
  }

  public void setMapFolderName(String mapFolderName) {
    this.mapFolderName.set(mapFolderName);
  }

  public StringProperty mapFolderNameProperty() {
    return mapFolderName;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }
}
