package com.faforever.client.domain;

import com.faforever.client.map.MapSize;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class MapVersionBean extends AbstractEntityBean<MapVersionBean> {

  @ToString.Include
  @EqualsAndHashCode.Include
  StringProperty folderName = new SimpleStringProperty();
  IntegerProperty gamesPlayed = new SimpleIntegerProperty(0);
  StringProperty description = new SimpleStringProperty();
  IntegerProperty maxPlayers = new SimpleIntegerProperty();
  ObjectProperty<MapSize> size = new SimpleObjectProperty<>();
  ObjectProperty<ComparableVersion> version = new SimpleObjectProperty<>();
  BooleanProperty hidden = new SimpleBooleanProperty();
  BooleanProperty ranked = new SimpleBooleanProperty();
  ObjectProperty<URL> downloadUrl = new SimpleObjectProperty<>();
  ObjectProperty<URL> thumbnailUrlSmall = new SimpleObjectProperty<>();
  ObjectProperty<URL> thumbnailUrlLarge = new SimpleObjectProperty<>();
  ObjectProperty<MapBean> map = new SimpleObjectProperty<>();
  ObservableList<MapVersionReviewBean> reviews = FXCollections.observableArrayList();

  public URL getDownloadUrl() {
    return downloadUrl.get();
  }

  public void setDownloadUrl(URL downloadUrl) {
    this.downloadUrl.set(downloadUrl);
  }

  public ObjectProperty<URL> downloadUrlProperty() {
    return downloadUrl;
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

  public int getGamesPlayed() {
    return gamesPlayed.get();
  }

  public void setGamesPlayed(int plays) {
    this.gamesPlayed.set(plays);
  }

  public IntegerProperty gamesPlayedProperty() {
    return gamesPlayed;
  }

  public MapSize getSize() {
    return size.get();
  }

  public ObjectProperty<MapSize> sizeProperty() {
    return size;
  }

  public void setSize(MapSize size) {
    this.size.set(size);
  }

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers.set(maxPlayers);
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
  }

  @Nullable
  public ComparableVersion getVersion() {
    return version.get();
  }

  public void setVersion(ComparableVersion version) {
    this.version.set(version);
  }

  public ObjectProperty<ComparableVersion> versionProperty() {
    return version;
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

  public URL getThumbnailUrlLarge() {
    return thumbnailUrlLarge.get();
  }

  public void setThumbnailUrlLarge(URL thumbnailUrlLarge) {
    this.thumbnailUrlLarge.set(thumbnailUrlLarge);
  }

  public ObjectProperty<URL> thumbnailUrlLargeProperty() {
    return thumbnailUrlLarge;
  }

  public URL getThumbnailUrlSmall() {
    return thumbnailUrlSmall.get();
  }

  public void setThumbnailUrlSmall(URL thumbnailUrlSmall) {
    this.thumbnailUrlSmall.set(thumbnailUrlSmall);
  }

  public ObjectProperty<URL> thumbnailUrlSmallProperty() {
    return thumbnailUrlSmall;
  }

  public boolean getHidden() {
    return hidden.get();
  }

  public void setHidden(boolean hidden) {
    this.hidden.set(hidden);
  }

  public BooleanProperty hiddenProperty() {
    return hidden;
  }

  public boolean getRanked() {
    return ranked.get();
  }

  public void setRanked(boolean ranked) {
    this.ranked.set(ranked);
  }

  public BooleanProperty rankedProperty() {
    return ranked;
  }

  public MapBean getMap() {
    return map.get();
  }

  public ObjectProperty<MapBean> mapProperty() {
    return map;
  }

  public void setMap(MapBean map) {
    this.map.set(map);
  }

  public ObservableList<MapVersionReviewBean> getReviews() {
    return reviews;
  }

  public void setReviews(List<MapVersionReviewBean> reviews) {
    if (reviews == null) {
      reviews = List.of();
    }
    this.reviews.setAll(reviews);
  }
}
