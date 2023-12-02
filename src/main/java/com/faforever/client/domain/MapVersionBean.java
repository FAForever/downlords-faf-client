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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MapVersionBean extends AbstractEntityBean {

  @ToString.Include
  @EqualsAndHashCode.Include
  private final StringProperty folderName = new SimpleStringProperty();
  private final IntegerProperty gamesPlayed = new SimpleIntegerProperty(0);
  private final StringProperty description = new SimpleStringProperty();
  private final IntegerProperty maxPlayers = new SimpleIntegerProperty();
  private final ObjectProperty<MapSize> size = new SimpleObjectProperty<>();
  private final ObjectProperty<ComparableVersion> version = new SimpleObjectProperty<>();
  private final BooleanProperty hidden = new SimpleBooleanProperty();
  private final BooleanProperty ranked = new SimpleBooleanProperty();
  private final ObjectProperty<URL> downloadUrl = new SimpleObjectProperty<>();
  private final ObjectProperty<URL> thumbnailUrlSmall = new SimpleObjectProperty<>();
  private final ObjectProperty<URL> thumbnailUrlLarge = new SimpleObjectProperty<>();
  private final ObjectProperty<MapBean> map = new SimpleObjectProperty<>();

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
}
