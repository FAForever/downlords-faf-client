package com.faforever.client.map;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.time.LocalDateTime;

public class MapBean implements Comparable<MapBean> {

  private final StringProperty folderName;
  private final StringProperty displayName;
  private final SimpleFloatProperty rating;
  private final IntegerProperty plays;
  private final StringProperty description;
  private final IntegerProperty downloads;
  private final IntegerProperty players;
  private final ObjectProperty<MapSize> size;
  private final IntegerProperty version;
  private final StringProperty id;
  private final StringProperty author;
  private final ObjectProperty<URL> downloadUrl;
  private final ObjectProperty<URL> smallThumbnailUrl;
  private final ObjectProperty<URL> largeThumbnailUrl;
  private final ObjectProperty<LocalDateTime> createTime;

  public MapBean() {
    this.id = new SimpleStringProperty();
    this.displayName = new SimpleStringProperty();
    this.folderName = new SimpleStringProperty();
    this.description = new SimpleStringProperty();
    this.plays = new SimpleIntegerProperty();
    this.downloads = new SimpleIntegerProperty();
    this.rating = new SimpleFloatProperty();
    this.players = new SimpleIntegerProperty();
    this.size = new SimpleObjectProperty<>();
    this.version = new SimpleIntegerProperty();
    this.smallThumbnailUrl = new SimpleObjectProperty<>();
    this.largeThumbnailUrl = new SimpleObjectProperty<>();
    this.downloadUrl = new SimpleObjectProperty<>();
    this.author = new SimpleStringProperty();
    this.createTime = new SimpleObjectProperty<>();
  }

  public static MapBean fromMap(com.faforever.client.api.Map map) {
    MapBean mapBean = new MapBean();
    mapBean.setDescription(map.getDescription());
    mapBean.setDisplayName(map.getDisplayName());
    mapBean.setFolderName(map.getTechnicalName());
    mapBean.setSize(new MapSize(map.getSizeX(), map.getSizeY()));
    mapBean.setDownloads(map.getDownloads());
    mapBean.setId(map.getId());
    mapBean.setPlayers(map.getMaxPlayers());
    mapBean.setRating(map.getRating());
    mapBean.setVersion(Integer.valueOf(map.getVersion()));
    mapBean.setDownloadUrl(map.getDownloadUrl());
    mapBean.setSmallThumbnailUrl(map.getThumbnailUrlSmall());
    mapBean.setLargeThumbnailUrl(map.getThumbnailUrlLarge());
    mapBean.setCreateTime(map.getCreateTime());
    return mapBean;
  }

  public String getAuthor() {
    return author.get();
  }

  public void setAuthor(String author) {
    this.author.set(author);
  }

  public StringProperty authorProperty() {
    return author;
  }

  public URL getDownloadUrl() {
    return downloadUrl.get();
  }

  public void setDownloadUrl(URL downloadUrl) {
    this.downloadUrl.set(downloadUrl);
  }

  public ObjectProperty<URL> downloadUrlProperty() {
    return downloadUrl;
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
  public int compareTo(@NotNull MapBean o) {
    return getDisplayName().compareTo(o.getDisplayName());
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public StringProperty idProperty() {
    return id;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public String getFolderName() {
    return folderName.get();
  }

  public void setFolderName(String folderName) {
    this.folderName.set(folderName);
  }

  public StringProperty folderNameProperty() {
    return folderName;
  }

  public URL getLargeThumbnailUrl() {
    return largeThumbnailUrl.get();
  }

  public void setLargeThumbnailUrl(URL largeThumbnailUrl) {
    this.largeThumbnailUrl.set(largeThumbnailUrl);
  }

  public ObjectProperty<URL> largeThumbnailUrlProperty() {
    return largeThumbnailUrl;
  }

  public URL getSmallThumbnailUrl() {
    return smallThumbnailUrl.get();
  }

  public void setSmallThumbnailUrl(URL smallThumbnailUrl) {
    this.smallThumbnailUrl.set(smallThumbnailUrl);
  }

  public ObjectProperty<URL> smallThumbnailUrlProperty() {
    return smallThumbnailUrl;
  }

  public LocalDateTime getCreateTime() {
    return createTime.get();
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime.set(createTime);
  }

  public ObjectProperty<LocalDateTime> createTimeProperty() {
    return createTime;
  }
}
