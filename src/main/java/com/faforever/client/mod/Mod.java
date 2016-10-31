package com.faforever.client.mod;

import com.faforever.client.api.dto.ModType;
import com.faforever.client.api.dto.ModVersion;
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
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class Mod {
  public static final Comparator<? super Mod> TIMES_PLAYED_COMPARATOR = Comparator.comparingInt(Mod::getPlayed);
  public static final Comparator<? super Mod> LIKES_COMPARATOR = Comparator.comparingInt(Mod::getLikes);
  public static final Comparator<? super Mod> PUBLISH_DATE_COMPARATOR = Comparator.comparing(Mod::getPublishDate);
  public static final Comparator<? super Mod> DOWNLOADS_COMPARATOR = Comparator.comparingInt(Mod::getDownloads);

  private final StringProperty name;
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
  private final StringProperty author;
  private final BooleanProperty selectable;
  private final BooleanProperty uiOnly;
  private final ObjectProperty<ComparableVersion> version;
  private final ObjectProperty<URL> thumbnailUrl;
  private final ListProperty<String> comments;
  private final BooleanProperty selected;
  private final IntegerProperty likes;
  private final IntegerProperty played;
  private final ObjectProperty<LocalDateTime> publishDate;
  private final IntegerProperty downloads;
  private final ObjectProperty<URL> downloadUrl;
  private final ListProperty<MountInfo> mountPoints;
  private final ListProperty<String> hookDirectories;
  private final ListProperty<Review> reviews;

  public Mod() {
    name = new SimpleStringProperty();
    imagePath = new SimpleObjectProperty<>();
    id = new SimpleStringProperty();
    uid = new SimpleStringProperty();
    description = new SimpleStringProperty();
    author = new SimpleStringProperty();
    selectable = new SimpleBooleanProperty();
    uiOnly = new SimpleBooleanProperty();
    version = new SimpleObjectProperty<>();
    selected = new SimpleBooleanProperty();
    likes = new SimpleIntegerProperty();
    played = new SimpleIntegerProperty();
    publishDate = new SimpleObjectProperty<>();
    downloads = new SimpleIntegerProperty();
    thumbnailUrl = new SimpleObjectProperty<>();
    comments = new SimpleListProperty<>(FXCollections.observableArrayList());
    downloadUrl = new SimpleObjectProperty<>();
    mountPoints = new SimpleListProperty<>(FXCollections.observableArrayList());
    hookDirectories = new SimpleListProperty<>(FXCollections.observableArrayList());
    reviews = new SimpleListProperty<>(FXCollections.observableArrayList(param
        -> new Observable[]{param.scoreProperty(), param.textProperty()}));
  }

  /**
   * @param basePath path to the directory where all the mod files are, used to resolve the path of the icon file.
   */
  static Mod fromModInfo(com.faforever.commons.mod.Mod modInfo, Path basePath) {
    Mod mod = new Mod();
    mod.setUid(modInfo.getUid());
    mod.setName(modInfo.getName());
    mod.setDescription(modInfo.getDescription());
    mod.setAuthor(modInfo.getAuthor());
    mod.setVersion(modInfo.getVersion());
    mod.setSelectable(modInfo.isSelectable());
    mod.setUiOnly(modInfo.isUiOnly());
    mod.getMountInfos().setAll(modInfo.getMountInfos());
    mod.getHookDirectories().setAll(modInfo.getHookDirectories());
    Optional.ofNullable(modInfo.getIcon())
        .map(icon -> Paths.get(icon))
        .filter(iconPath -> iconPath.getNameCount() > 2)
        .ifPresent(iconPath -> mod.setImagePath(basePath.resolve(iconPath.subpath(2, iconPath.getNameCount()))));
    return mod;
  }

  public static Mod fromModInfo(com.faforever.client.api.dto.Mod mod) {
    ModVersion modVersion = mod.getLatestVersion();

    Mod modInfoBean = new Mod();
    modInfoBean.setUiOnly(modVersion.getType() == ModType.UI);
    modInfoBean.setName(mod.getDisplayName());
    modInfoBean.setAuthor(mod.getAuthor());
    modInfoBean.setVersion(new ComparableVersion(String.valueOf(modVersion.getVersion())));
//    modInfoBean.setLikes(modVersion.getLikes());
//    modInfoBean.setPlayed(mod.getPlayed());
    modInfoBean.setPublishDate(modVersion.getCreateTime().toLocalDateTime());
    modInfoBean.setDescription(modVersion.getDescription());
    modInfoBean.setId(modVersion.getId());
    modInfoBean.setUid(modVersion.getUid());
//    modInfoBean.setDownloads(mod));
    modInfoBean.setThumbnailUrl(modVersion.getThumbnailUrl());
//    modInfoBean.getComments().setAll(mod.getComments());
    modInfoBean.setDownloadUrl(modVersion.getDownloadUrl());
    return modInfoBean;
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

  public String getAuthor() {
    return author.get();
  }

  public void setAuthor(String author) {
    this.author.set(author);
  }

  public StringProperty authorProperty() {
    return author;
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

  public boolean getUiOnly() {
    return uiOnly.get();
  }

  public void setUiOnly(boolean uiOnly) {
    this.uiOnly.set(uiOnly);
  }

  public BooleanProperty uiOnlyProperty() {
    return uiOnly;
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

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
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

  public LocalDateTime getPublishDate() {
    return publishDate.get();
  }

  public void setPublishDate(LocalDateTime publishDate) {
    this.publishDate.set(publishDate);
  }

  public ObjectProperty<LocalDateTime> publishDateProperty() {
    return publishDate;
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Mod that = (Mod) o;
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
}
