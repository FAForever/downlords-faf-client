package com.faforever.client.domain;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
public class MapBean extends AbstractEntityBean<MapBean> {

  @ToString.Include
  @EqualsAndHashCode.Include
  StringProperty displayName = new SimpleStringProperty();
  IntegerProperty gamesPlayed = new SimpleIntegerProperty(0);
  ObjectProperty<PlayerBean> author = new SimpleObjectProperty<>();
  BooleanProperty recommended = new SimpleBooleanProperty();
  ObjectProperty<MapType> mapType = new SimpleObjectProperty<>();
  ObjectProperty<MapVersionBean> latestVersion = new SimpleObjectProperty<>();
  ObjectProperty<MapReviewsSummaryBean> mapReviewsSummary = new SimpleObjectProperty<>();
  ObservableList<MapVersionBean> versions = FXCollections.observableArrayList();

  public PlayerBean getAuthor() {
    return author.get();
  }

  public void setAuthor(PlayerBean author) {
    this.author.set(author);
  }

  public ObjectProperty<PlayerBean> authorProperty() {
    return author;
  }

  public MapVersionBean getLatestVersion() {
    return latestVersion.get();
  }

  public void setLatestVersion(MapVersionBean latestVersion) {
    this.latestVersion.set(latestVersion);
  }

  public ObjectProperty<MapVersionBean> latestVersionProperty() {
    return latestVersion;
  }

  public StringProperty displayNameProperty() {
    return displayName;
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

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public MapReviewsSummaryBean getMapReviewsSummary() {
    return mapReviewsSummary.get();
  }

  public void setMapReviewsSummary(MapReviewsSummaryBean mapReviewsSummary) {
    this.mapReviewsSummary.set(mapReviewsSummary);
  }

  public ObjectProperty<MapReviewsSummaryBean> mapReviewsSummaryProperty() {
    return mapReviewsSummary;
  }

  public ObservableList<MapVersionBean> getVersions() {
    return versions;
  }

  public void setVersions(List<MapVersionBean> versions) {
    if (versions == null) {
      versions = List.of();
    }
    this.versions.setAll(versions);
  }

  public boolean getRecommended() {
    return recommended.get();
  }

  public void setRecommended(boolean recommended) {
    this.recommended.set(recommended);
  }

  public BooleanProperty recommendedProperty() {
    return recommended;
  }

  public boolean isRecommended() {
    return recommended.get();
  }

  public MapType getMapType() {
    return mapType.get();
  }

  public ObjectProperty<MapType> mapTypeProperty() {
    return mapType;
  }

  public void setMapType(MapType mapType) {
    this.mapType.set(mapType);
  }

  @RequiredArgsConstructor
  public enum MapType {
    SKIRMISH("skirmish"),
    COOP("campaign_coop"),
    OTHER(null);

    private static final Map<String, MapType> fromString;

    static {
      fromString = new HashMap<>();
      for (MapType mapType : values()) {
        fromString.put(mapType.string, mapType);
      }
    }

    @Getter
    private final String string;

    public static MapType fromString(String mapType) {
      if (fromString.containsKey(mapType)) {
        return fromString.get(mapType);
      }
      return OTHER;
    }
  }
}
