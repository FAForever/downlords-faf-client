package com.faforever.client.mod;

import com.faforever.client.vault.review.Review;
import com.faforever.commons.mod.MountInfo;
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
import lombok.Getter;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModVersion {
  private final StringProperty displayName;
  private final ObjectProperty<Path> imagePath;
  /**
   * Entity ID as provided by the API (DB primary key).
   */
  private final StringProperty id;
  /**
   * UID as specified in the mod itself (specified by the uploader).
   */
  private final StringProperty uid;
  private final StringProperty description;
  private final StringProperty uploader;
  private final BooleanProperty selectable;
  private final ObjectProperty<ComparableVersion> version;
  private final ObjectProperty<URL> thumbnailUrl;
  private final ListProperty<String> comments;
  private final BooleanProperty selected;
  private final IntegerProperty likes;
  private final IntegerProperty played;
  private final ObjectProperty<LocalDateTime> createTime;
  private final ObjectProperty<LocalDateTime> updateTime;
  private final ObjectProperty<URL> downloadUrl;
  private final ListProperty<MountInfo> mountPoints;
  private final ListProperty<String> hookDirectories;
  private final ListProperty<Review> reviews;
  private final ObjectProperty<ReviewsSummary> reviewsSummary;
  private final ObjectProperty<ModType> modType;
  private final StringProperty filename;
  private final StringProperty icon;
  private final BooleanProperty ranked;
  private final BooleanProperty hidden;
  private final ObjectProperty<Mod> mod;

  public ModVersion() {
    displayName = new SimpleStringProperty();
    imagePath = new SimpleObjectProperty<>();
    id = new SimpleStringProperty();
    uid = new SimpleStringProperty();
    description = new SimpleStringProperty();
    uploader = new SimpleStringProperty();
    selectable = new SimpleBooleanProperty();
    version = new SimpleObjectProperty<>();
    selected = new SimpleBooleanProperty();
    likes = new SimpleIntegerProperty();
    played = new SimpleIntegerProperty();
    createTime = new SimpleObjectProperty<>();
    updateTime = new SimpleObjectProperty<>();
    reviewsSummary = new SimpleObjectProperty<>();
    thumbnailUrl = new SimpleObjectProperty<>();
    comments = new SimpleListProperty<>(FXCollections.observableArrayList());
    downloadUrl = new SimpleObjectProperty<>();
    mountPoints = new SimpleListProperty<>(FXCollections.observableArrayList());
    hookDirectories = new SimpleListProperty<>(FXCollections.observableArrayList());
    reviews = new SimpleListProperty<>(FXCollections.observableArrayList(param
        -> new Observable[]{param.scoreProperty(), param.textProperty()}));
    modType = new SimpleObjectProperty<>();
    filename = new SimpleStringProperty();
    icon = new SimpleStringProperty();
    ranked = new SimpleBooleanProperty();
    hidden = new SimpleBooleanProperty();
    mod = new SimpleObjectProperty<>();
  }

  /**
   * @param basePath path to the directory where all the mod files are, used to resolve the path of the icon file.
   */
  static ModVersion fromModInfo(com.faforever.commons.mod.Mod modInfo, Path basePath) {
    ModVersion modVersion = new ModVersion();
    modVersion.setUid(modInfo.getUid());
    modVersion.setDisplayName(modInfo.getName());
    modVersion.setDescription(modInfo.getDescription());
    modVersion.setUploader(modInfo.getAuthor());
    modVersion.setVersion(modInfo.getVersion());
    modVersion.setSelectable(modInfo.isSelectable());
    modVersion.setModType(modInfo.isUiOnly() ? ModType.UI : ModType.SIM);
    modVersion.getMountInfos().setAll(modInfo.getMountInfos());
    modVersion.getHookDirectories().setAll(modInfo.getHookDirectories());
    Optional.ofNullable(modInfo.getIcon())
        .map(icon -> Paths.get(icon))
        .filter(iconPath -> iconPath.getNameCount() > 2)
        .ifPresent(iconPath -> modVersion.setImagePath(basePath.resolve(iconPath.subpath(2, iconPath.getNameCount()))));
    return modVersion;
  }

  public static ModVersion fromDto(com.faforever.client.api.dto.ModVersion dto, Mod parent) {
    ModVersion modVersionVersion = new ModVersion();
    modVersionVersion.setVersion(dto.getVersion());
    modVersionVersion.setId(dto.getId());
    modVersionVersion.setUid(dto.getUid());
    modVersionVersion.setModType(ModType.fromDto(dto.getType()));
    modVersionVersion.setDescription(dto.getDescription());
    modVersionVersion.setFilename(dto.getFilename());
    modVersionVersion.setIcon(dto.getIcon());
    modVersionVersion.setRanked(dto.isRanked());
    modVersionVersion.setHidden(dto.isHidden());
    Optional.ofNullable(dto.getCreateTime())
        .ifPresent(offsetDateTime -> modVersionVersion.setCreateTime(offsetDateTime.toLocalDateTime()));
    Optional.ofNullable(dto.getUpdateTime())
        .ifPresent(offsetDateTime -> modVersionVersion.setUpdateTime(offsetDateTime.toLocalDateTime()));
    modVersionVersion.setThumbnailUrl(dto.getThumbnailUrl());
    modVersionVersion.setDownloadUrl(dto.getDownloadUrl());
    Mod mod = Optional.ofNullable(parent)
        .orElseGet(() -> Mod.fromDto(dto.getMod()));
    modVersionVersion.setMod(mod);
    Optional.ofNullable(mod.getUploader()).ifPresent(uploader -> modVersionVersion.setUploader(uploader.getLogin()));
    return modVersionVersion;
  }

  public static ModVersion fromModDto(com.faforever.client.api.dto.Mod dto) {
    com.faforever.client.api.dto.ModVersion modVersionVersion = dto.getLatestVersion();

    ModVersion modVersion = new ModVersion();
    Optional.ofNullable(dto.getUploader()).ifPresent(uploader -> modVersion.setUploader(uploader.getLogin()));
    modVersion.setDescription(modVersionVersion.getDescription());
    modVersion.setDisplayName(dto.getDisplayName());
    modVersion.setId(modVersionVersion.getId());
    modVersion.setUid(modVersionVersion.getUid());
    modVersion.setVersion(modVersionVersion.getVersion());
    modVersion.setDownloadUrl(modVersionVersion.getDownloadUrl());
    modVersion.setThumbnailUrl(modVersionVersion.getThumbnailUrl());
    modVersion.setReviewsSummary(ReviewsSummary.fromDto(modVersionVersion.getModVersionReviewsSummary()));
    modVersion.setCreateTime(modVersionVersion.getCreateTime().toLocalDateTime());
    Optional.ofNullable(dto.getUpdateTime()).map(OffsetDateTime::toLocalDateTime).ifPresent(modVersion::setUpdateTime);
    modVersion.getReviews().setAll(
        dto.getVersions().stream()
            .filter(v -> v.getReviews() != null)
            .flatMap(v -> v.getReviews().parallelStream())
            .map(Review::fromDto)
            .collect(Collectors.toList()));
    modVersion.setModType(ModType.fromDto(modVersionVersion.getType()));
    modVersion.setMod(Mod.fromDto(dto));
    return modVersion;
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

  public boolean getSelected() {
    return selected.get();
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }

  public String getUploader() {
    return uploader.get();
  }

  public void setUploader(String uploader) {
    this.uploader.set(uploader);
  }

  public boolean getSelectable() {
    return selectable.get();
  }

  public void setSelectable(boolean selectable) {
    this.selectable.set(selectable);
  }

  public BooleanProperty selectableProperty() {
    return selectable;
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

  public ComparableVersion getVersion() {
    return version.get();
  }

  public void setVersion(ComparableVersion version) {
    this.version.set(version);
  }

  public ObjectProperty<ComparableVersion> versionProperty() {
    return version;
  }

  public Path getImagePath() {
    return imagePath.get();
  }

  public void setImagePath(Path imagePath) {
    this.imagePath.set(imagePath);
  }

  public ObjectProperty<Path> imagePathProperty() {
    return imagePath;
  }

  public StringProperty uploaderProperty() {
    return uploader;
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  /**
   * The ID within the database. {@code null} in case the mod was loaded locally.
   */
  @Nullable
  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }

  public int getLikes() {
    return likes.get();
  }

  public void setLikes(int likes) {
    this.likes.set(likes);
  }

  public IntegerProperty likesProperty() {
    return likes;
  }

  public int getPlayed() {
    return played.get();
  }

  public void setPlayed(int played) {
    this.played.set(played);
  }

  public IntegerProperty playedProperty() {
    return played;
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public LocalDateTime getCreateTime() {
    return createTime.get();
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime.set(createTime);
  }

  public URL getThumbnailUrl() {
    return thumbnailUrl.get();
  }

  public void setThumbnailUrl(URL thumbnailUrl) {
    this.thumbnailUrl.set(thumbnailUrl);
  }

  public ObjectProperty<URL> thumbnailUrlProperty() {
    return thumbnailUrl;
  }

  public ListProperty<String> commentsProperty() {
    return comments;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid.get());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ModVersion && ((ModVersion) o).getUid() != null && getUid() != null) {
      return ((ModVersion) o).getUid().equals(this.getUid());
    }
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModVersion that = (ModVersion) o;
    return Objects.equals(uid.get(), that.uid.get());
  }

  public ObservableList<String> getComments() {
    return comments.get();
  }

  public void setComments(ObservableList<String> comments) {
    this.comments.set(comments);
  }

  public ObservableList<MountInfo> getMountInfos() {
    return mountPoints.get();
  }

  public ObservableList<String> getHookDirectories() {
    return hookDirectories.get();
  }

  public ObservableList<Review> getReviews() {
    return reviews.get();
  }

  public void setReviews(ObservableList<Review> reviews) {
    this.reviews.set(reviews);
  }

  public ListProperty<Review> reviewsProperty() {
    return reviews;
  }

  public String getUid() {
    return uid.get();
  }

  public void setUid(String uid) {
    this.uid.set(uid);
  }

  public StringProperty uidProperty() {
    return uid;
  }

  public ObjectProperty<LocalDateTime> createTimeProperty() {
    return createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime.get();
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime.set(updateTime);
  }

  public ObjectProperty<LocalDateTime> updateTimeProperty() {
    return updateTime;
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

  public ModType getModType() {
    return modType.get();
  }

  public void setModType(ModType modType) {
    this.modType.set(modType);
  }

  public ObjectProperty<ModType> modTypeProperty() {
    return modType;
  }

  public String getFilename() {
    return filename.get();
  }

  public void setFilename(String filename) {
    this.filename.set(filename);
  }

  public StringProperty filenameProperty() {
    return filename;
  }

  public String getIcon() {
    return icon.get();
  }

  public void setIcon(String icon) {
    this.icon.set(icon);
  }

  public StringProperty iconProperty() {
    return icon;
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

  public boolean isHidden() {
    return hidden.get();
  }

  public void setHidden(boolean hidden) {
    this.hidden.set(hidden);
  }

  public BooleanProperty hiddenProperty() {
    return hidden;
  }

  public Mod getMod() {
    return mod.get();
  }

  public void setMod(Mod mod) {
    this.mod.set(mod);
  }

  public ObjectProperty<Mod> modProperty() {
    return mod;
  }

  public enum ModType {
    UI("modType.ui"),
    SIM("modType.sim");

    @Getter
    private final String i18nKey;

    ModType(String i18nKey) {
      this.i18nKey = i18nKey;
    }

    public static ModType fromDto(com.faforever.client.api.dto.ModType modType) {
      return modType == com.faforever.client.api.dto.ModType.UI ? UI : SIM;
    }
  }
}
