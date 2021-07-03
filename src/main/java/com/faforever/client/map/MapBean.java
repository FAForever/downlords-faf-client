package com.faforever.client.map;

import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewsSummary;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

public class MapBean implements Comparable<MapBean> {

  private final StringProperty folderName;
  private final StringProperty displayName;
  private final IntegerProperty mapGamesPlayed;
  private final IntegerProperty mapVersionGamesPlayed;
  private final StringProperty description;
  private final IntegerProperty players;
  private final ObjectProperty<MapSize> size;
  private final ObjectProperty<ComparableVersion> version;
  private final StringProperty id;
  private final StringProperty author;
  private final BooleanProperty hidden;
  private final BooleanProperty ranked;
  private final ObjectProperty<URL> downloadUrl;
  private final ObjectProperty<URL> smallThumbnailUrl;
  private final ObjectProperty<URL> largeThumbnailUrl;
  private final ObjectProperty<LocalDateTime> createTime;
  private final ObjectProperty<Type> type;
  private final ObjectProperty<ReviewsSummary> reviewsSummary;
  private final ListProperty<Review> reviews;

  public MapBean() {
    id = new SimpleStringProperty();
    displayName = new SimpleStringProperty();
    folderName = new SimpleStringProperty();
    description = new SimpleStringProperty();
    mapGamesPlayed = new SimpleIntegerProperty(0);
    mapVersionGamesPlayed = new SimpleIntegerProperty(0);
    players = new SimpleIntegerProperty();
    size = new SimpleObjectProperty<>();
    version = new SimpleObjectProperty<>();
    smallThumbnailUrl = new SimpleObjectProperty<>();
    largeThumbnailUrl = new SimpleObjectProperty<>();
    downloadUrl = new SimpleObjectProperty<>();
    author = new SimpleStringProperty();
    createTime = new SimpleObjectProperty<>();
    type = new SimpleObjectProperty<>();
    reviewsSummary = new SimpleObjectProperty<>();
    reviews = new SimpleListProperty<>(FXCollections.observableArrayList(param
        -> new Observable[]{param.scoreProperty(), param.textProperty()}));
    hidden = new SimpleBooleanProperty();
    ranked = new SimpleBooleanProperty();
  }

  public static MapBean fromMapDto(com.faforever.commons.api.dto.Map dto) {
    MapVersion mapVersion = dto.getLatestVersion();

    MapBean mapBean = new MapBean();
    Optional.ofNullable(dto.getAuthor()).ifPresent(author -> mapBean.setAuthor(author.getLogin()));
    mapBean.setDisplayName(dto.getDisplayName());
    mapBean.setMapGamesPlayed(dto.getGamesPlayed());
    setPropertiesFromMapVersion(mapVersion, mapBean);
    mapBean.setReviewsSummary(ReviewsSummary.fromDto(dto.getMapReviewsSummary()));
    dto.getVersions().forEach(v -> {
      if (v.getReviews() != null) {
        v.getReviews().forEach(mapVersionReview -> {
          Review review = Review.fromDto(mapVersionReview);
          review.setVersion(v.getVersion());
          review.setLatestVersion(dto.getLatestVersion().getVersion());
          mapBean.getReviews().add(review);
        });
      }
    });
    return mapBean;
  }

  public static MapBean fromMapVersionDto(com.faforever.commons.api.dto.MapVersion mapVersion) {
    MapBean mapBean = new MapBean();
    Optional.ofNullable(mapVersion.getMap().getAuthor()).ifPresent(author -> mapBean.setAuthor(author.getLogin()));
    mapBean.setDisplayName(mapVersion.getMap().getDisplayName());
    mapBean.setMapGamesPlayed(mapVersion.getMap().getGamesPlayed());
    setPropertiesFromMapVersion(mapVersion, mapBean);
    mapBean.setReviewsSummary(ReviewsSummary.fromDto(mapVersion.getMap().getMapReviewsSummary()));
    mapVersion.getMap().getVersions().forEach(v -> {
      if (v.getReviews() != null) {
        v.getReviews().forEach(mapVersionReview -> {
          Review review = Review.fromDto(mapVersionReview);
          review.setVersion(v.getVersion());
          review.setLatestVersion(mapVersion.getMap().getLatestVersion().getVersion());
          mapBean.getReviews().add(review);
        });
      }
    });
    return mapBean;
  }

  private static void setPropertiesFromMapVersion(MapVersion mapVersion, MapBean mapBean) {
    mapBean.setDescription(mapVersion.getDescription());
    mapBean.setFolderName(mapVersion.getFolderName());
    mapBean.setSize(MapSize.valueOf(mapVersion.getWidth(), mapVersion.getHeight()));
    mapBean.setId(mapVersion.getId());
    mapBean.setPlayers(mapVersion.getMaxPlayers());
    mapBean.setVersion(mapVersion.getVersion());
    mapBean.setDownloadUrl(mapVersion.getDownloadUrl());
    mapBean.setSmallThumbnailUrl(mapVersion.getThumbnailUrlSmall());
    mapBean.setLargeThumbnailUrl(mapVersion.getThumbnailUrlLarge());
    mapBean.setCreateTime(mapVersion.getCreateTime().toLocalDateTime());
    mapBean.setMapVersionGamesPlayed(mapVersion.getGamesPlayed());
    mapBean.setHidden(mapVersion.isHidden());
    mapBean.setRanked(mapVersion.isRanked());
  }

  public static MapBean fromNeroxisGeneratedMapParams(NeroxisGeneratorParams mapParams) {
    MapBean mapBean = new MapBean();
    mapBean.setAuthor("Neroxis");
    mapBean.setDescription("");
    mapBean.setDisplayName(String.format("neroxis_map_generator_%s_mapSize=%dkm_spawns=%d", mapParams.getVersion(), (int) (mapParams.getSize() / 51.2), mapParams.getSpawns()));
    mapBean.setFolderName(mapBean.getDisplayName());
    mapBean.setSize(MapSize.valueOf(mapParams.getSize(), mapParams.getSize()));
    mapBean.setId(mapBean.getDisplayName());
    mapBean.setPlayers(mapParams.getSpawns());
    mapBean.setVersion(new ComparableVersion("1"));
    mapBean.setReviewsSummary(new ReviewsSummary());
    mapBean.setHidden(false);
    mapBean.setRanked(true);
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

  public int getMapGamesPlayed() {
    return mapGamesPlayed.get();
  }

  public void setMapGamesPlayed(int plays) {
    this.mapGamesPlayed.set(plays);
  }

  public IntegerProperty mapGamesPlayedProperty() {
    return mapGamesPlayed;
  }

  public int getMapVersionGamesPlayed() {
    return mapVersionGamesPlayed.get();
  }

  public void setMapVersionGamesPlayed(int plays) {
    this.mapVersionGamesPlayed.set(plays);
  }

  public IntegerProperty mapVersionGamesPlayedProperty() {
    return mapVersionGamesPlayed;
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

  public ReviewsSummary getReviewsSummary() {
    return reviewsSummary.get();
  }

  public void setReviewsSummary(ReviewsSummary reviewsSummary) {
    this.reviewsSummary.set(reviewsSummary);
  }

  public ObjectProperty<ReviewsSummary> reviewsSummaryProperty() {
    return reviewsSummary;
  }

  public ObservableList<Review> getReviews() {
    return reviews.get();
  }

  public ListProperty<Review> reviewsProperty() {
    return reviews;
  }

  public boolean isHidden() {
    return hidden.get();
  }

  public void setHidden(boolean hidden) {
    this.hidden.set(hidden);
  }

  public BooleanProperty hiddenProperty() {
    return hidden;
  }

  public boolean isRanked() {
    return ranked.get();
  }

  public void setRanked(boolean ranked) {
    this.ranked.set(ranked);
  }

  public BooleanProperty rankedProperty() {
    return ranked;
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

    private final String string;

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
