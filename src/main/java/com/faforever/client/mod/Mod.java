package com.faforever.client.mod;

import com.faforever.client.api.dto.ModType;
import com.faforever.client.api.dto.ModVersion;
import com.faforever.commons.mod.MountPoint;
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

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

public class Mod {
  public static final Comparator<? super Mod> TIMES_PLAYED_COMPARATOR = Comparator.comparingInt(Mod::getPlayed);
  public static final Comparator<? super Mod> LIKES_COMPARATOR = Comparator.comparingInt(Mod::getLikes);
  public static final Comparator<? super Mod> PUBLISH_DATE_COMPARATOR = Comparator.comparing(Mod::getPublishDate);
  public static final Comparator<? super Mod> DOWNLOADS_COMPARATOR = Comparator.comparingInt(Mod::getDownloads);
  private final StringProperty name;
  private final ObjectProperty<Path> imagePath;
  private final StringProperty id;
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
  private final ListProperty<MountPoint> mountPoints;
  private final ListProperty<String> hookDirectories;

  public Mod() {
    name = new SimpleStringProperty();
    imagePath = new SimpleObjectProperty<>();
    id = new SimpleStringProperty();
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
  }

  public static Mod fromDto(com.faforever.commons.mod.Mod modInfo) {
    Mod mod = new Mod();
    mod.setId(modInfo.getId());
    mod.setName(modInfo.getName());
    mod.setDescription(modInfo.getDescription());
    mod.setAuthor(modInfo.getAuthor());
    mod.setVersion(modInfo.getVersion());
    mod.setSelectable(modInfo.isSelectable());
    mod.setUiOnly(modInfo.isUiOnly());
    mod.setImagePath(modInfo.getImagePath());
    mod.getMountPoints().setAll(modInfo.getMountPoints());
    mod.getHookDirectories().setAll(modInfo.getHookDirectories());
    return mod;
  }

  public static Mod fromDto(com.faforever.client.api.dto.Mod mod) {
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
    return Objects.hash(id.get());
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
    return Objects.equals(id.get(), that.id.get());
  }

  public ObservableList<String> getComments() {
    return comments.get();
  }

  public void setComments(ObservableList<String> comments) {
    this.comments.set(comments);
  }

  public ObservableList<MountPoint> getMountPoints() {
    return mountPoints.get();
  }

  public void setMountPoints(ObservableList<MountPoint> mountPoints) {
    this.mountPoints.set(mountPoints);
  }

  public ListProperty<MountPoint> mountPointsProperty() {
    return mountPoints;
  }

  public ObservableList<String> getHookDirectories() {
    return hookDirectories.get();
  }

  public void setHookDirectories(ObservableList<String> hookDirectories) {
    this.hookDirectories.set(hookDirectories);
  }

  public ListProperty<String> hookDirectoriesProperty() {
    return hookDirectories;
  }
}
