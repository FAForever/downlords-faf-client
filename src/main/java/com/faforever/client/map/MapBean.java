package com.faforever.client.map;

import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.vault.review.Review;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MapBean implements Comparable<MapBean> {

  private final StringProperty folderName;
  private final StringProperty displayName;
  private final IntegerProperty numberOfPlays;
  private final StringProperty description;
  private final IntegerProperty downloads;
  private final IntegerProperty players;
  private final ObjectProperty<MapSize> size;
  private final ObjectProperty<ComparableVersion> version;
  private final StringProperty id;
  private final StringProperty author;
  private final ObjectProperty<URL> downloadUrl;
  private final ObjectProperty<URL> smallThumbnailUrl;
  private final ObjectProperty<URL> largeThumbnailUrl;
  private final ObjectProperty<LocalDateTime> createTime;
  private final ObjectProperty<Type> type;
  private final ListProperty<Review> reviews;

  public MapBean() {
    id = new SimpleStringProperty();
    displayName = new SimpleStringProperty();
    folderName = new SimpleStringProperty();
    description = new SimpleStringProperty();
    numberOfPlays = new SimpleIntegerProperty();
    downloads = new SimpleIntegerProperty();
    players = new SimpleIntegerProperty();
    size = new SimpleObjectProperty<>();
    version = new SimpleObjectProperty<>();
    smallThumbnailUrl = new SimpleObjectProperty<>();
    largeThumbnailUrl = new SimpleObjectProperty<>();
    downloadUrl = new SimpleObjectProperty<>();
    author = new SimpleStringProperty();
    createTime = new SimpleObjectProperty<>();
    type = new SimpleObjectProperty<>();
    reviews = new SimpleListProperty<>(FXCollections.observableArrayList(param
        -> new Observable[]{param.scoreProperty(), param.textProperty()}));
  }

  public static MapBean fromMapDto(com.faforever.client.api.dto.Map map) {
    MapVersion mapVersion = map.getLatestVersion();

    MapBean mapBean = new MapBean();
    Optional.ofNullable(map.getAuthor()).ifPresent(author -> mapBean.setAuthor(author.getLogin()));
    mapBean.setDescription(mapVersion.getDescription());
    mapBean.setDisplayName(map.getDisplayName());
    mapBean.setFolderName(mapVersion.getFolderName());
    mapBean.setSize(MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()));
    mapBean.setDownloads(map.getStatistics().getDownloads());
    mapBean.setId(mapVersion.getId());
    mapBean.setPlayers(mapVersion.getMaxPlayers());
    mapBean.setVersion(mapVersion.getVersion());
    mapBean.setDownloadUrl(mapVersion.getDownloadUrl());
    mapBean.setSmallThumbnailUrl(mapVersion.getThumbnailUrlSmall());
    mapBean.setLargeThumbnailUrl(mapVersion.getThumbnailUrlLarge());
    mapBean.setCreateTime(mapVersion.getCreateTime().toLocalDateTime());
    mapBean.setNumberOfPlays(map.getStatistics().getPlays());
    mapBean.getReviews().setAll(
        map.getVersions().stream()
            .filter(v -> v.getReviews() != null)
            .flatMap(v -> v.getReviews().parallelStream())
            .map(Review::fromDto)
            .collect(Collectors.toList()));
    return mapBean;
  }

  public static MapBean fromMapVersionDto(com.faforever.client.api.dto.MapVersion mapVersion) {
    MapBean mapBean = new MapBean();
    Optional.ofNullable(mapVersion.getMap().getAuthor()).ifPresent(author -> mapBean.setAuthor(author.getLogin()));
    mapBean.setDescription(mapVersion.getDescription());
    mapBean.setDisplayName(mapVersion.getMap().getDisplayName());
    mapBean.setFolderName(mapVersion.getFolderName());
    mapBean.setSize(MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()));
    mapBean.setDownloads(mapVersion.getMap().getStatistics().getDownloads());
    mapBean.setId(mapVersion.getId());
    mapBean.setPlayers(mapVersion.getMaxPlayers());
    mapBean.setVersion(mapVersion.getVersion());
    mapBean.setDownloadUrl(mapVersion.getDownloadUrl());
    mapBean.setSmallThumbnailUrl(mapVersion.getThumbnailUrlSmall());
    mapBean.setLargeThumbnailUrl(mapVersion.getThumbnailUrlLarge());
    mapBean.setCreateTime(mapVersion.getCreateTime().toLocalDateTime());
    mapBean.setNumberOfPlays(mapVersion.getMap().getStatistics().getPlays());
    mapBean.getReviews().setAll(mapVersion.getReviews().parallelStream()
        .map(Review::fromDto)
        .collect(Collectors.toList()));
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

  public int getNumberOfPlays() {
    return numberOfPlays.get();
  }

  public void setNumberOfPlays(int plays) {
    this.numberOfPlays.set(plays);
  }

  public IntegerProperty numberOfPlaysProperty() {
    return numberOfPlays;
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

  public Type getType() {
    return type.get();
  }

  public void setType(Type type) {
    this.type.set(type);
  }

  public ObjectProperty<Type> typeProperty() {
    return type;
  }

  public ObservableList<Review> getReviews() {
    return reviews.get();
  }

  public ListProperty<Review> reviewsProperty() {
    return reviews;
  }

  public enum Type {
    SKIRMISH("skirmish"),
    COOP("campaign_coop"),
    OTHER(null);

    private static final Map<String, Type> fromString;

    static {
      fromString = new HashMap<>();
      for (Type type : values()) {
        fromString.put(type.string, type);
      }
    }

    private String string;

    Type(String string) {
      this.string = string;
    }

    public static Type fromString(String type) {
      if (fromString.containsKey(type)) {
        return fromString.get(type);
      }
      return OTHER;
    }
  }
}
