package com.faforever.client.mod;

import com.faforever.client.legacy.domain.ModInfo;
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

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

import static com.faforever.client.util.TimeUtil.fromPythonTime;

public class ModInfoBean {

  public static final String FIELD_ID = "id";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_AUTHOR = "author";
  public static final Comparator<? super ModInfoBean> UI_LIKES_COMPARATOR = (o1, o2) -> {
    if (o1.uiOnly.get() && !o2.uiOnly.get()) {
      return -1;
    }
    return Integer.compare(o1.getLikes(), o2.getLikes());
  };
  public static final Comparator<? super ModInfoBean> LIKES_COMPARATOR = (o1, o2) -> Integer.compare(o1.getLikes(), o2.getLikes());
  public static final Comparator<? super ModInfoBean> PUBLISH_DATE_COMPARATOR = (o1, o2) -> o1.getPublishDate().compareTo(o2.getPublishDate());
  public static final Comparator<? super ModInfoBean> DOWNLOADS_COMPARATOR = (o1, o2) -> Integer.compare(o1.getDownloads(), o2.getDownloads());
  private final StringProperty name;
  private final ObjectProperty<Path> imagePath;
  private final StringProperty uid;
  private final StringProperty description;
  private final StringProperty author;
  private final BooleanProperty selectable;
  private final BooleanProperty uiOnly;
  private final StringProperty version;
  private final StringProperty thumbnailUrl;
  private final ListProperty<String> comments;
  private final BooleanProperty selected;
  private final IntegerProperty likes;
  private final IntegerProperty played;
  private final ObjectProperty<Instant> publishDate;
  private final IntegerProperty downloads;
  private final ObjectProperty<URL> downloadUrl;

  public ModInfoBean() {
    this(null, null, null, null);
  }

  public ModInfoBean(String uid, String name, String description, String author) {
    this.name = new SimpleStringProperty(name);
    imagePath = new SimpleObjectProperty<>();
    this.uid = new SimpleStringProperty(uid);
    this.description = new SimpleStringProperty(description);
    this.author = new SimpleStringProperty(author);
    selectable = new SimpleBooleanProperty();
    uiOnly = new SimpleBooleanProperty();
    version = new SimpleStringProperty();
    selected = new SimpleBooleanProperty();
    likes = new SimpleIntegerProperty();
    played = new SimpleIntegerProperty();
    publishDate = new SimpleObjectProperty<>();
    downloads = new SimpleIntegerProperty();
    thumbnailUrl = new SimpleStringProperty();
    comments = new SimpleListProperty<>(FXCollections.observableArrayList());
    downloadUrl = new SimpleObjectProperty<>();
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

  public String getVersion() {
    return version.get();
  }

  public void setVersion(String version) {
    this.version.set(version);
  }

  public StringProperty versionProperty() {
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

  public String getUid() {
    return uid.get();
  }

  public void setUid(String uid) {
    this.uid.set(uid);
  }

  public StringProperty uidProperty() {
    return uid;
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

  public Instant getPublishDate() {
    return publishDate.get();
  }

  public void setPublishDate(Instant publishDate) {
    this.publishDate.set(publishDate);
  }

  public ObjectProperty<Instant> publishDateProperty() {
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

  public String getThumbnailUrl() {
    return thumbnailUrl.get();
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl.set(thumbnailUrl);
  }

  public StringProperty thumbnailUrlProperty() {
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
    ModInfoBean that = (ModInfoBean) o;
    return Objects.equals(uid.get(), that.uid.get());
  }

  public static ModInfoBean fromModInfo(ModInfo modInfo) {
    ModInfoBean modInfoBean = new ModInfoBean();
    modInfoBean.setUiOnly(modInfo.isUi() == 1);
    modInfoBean.setName(modInfo.getName());
    modInfoBean.setAuthor(modInfo.getAuthor());
    modInfoBean.setVersion(modInfo.getVersion());
    modInfoBean.setLikes(modInfo.getLikes());
    modInfoBean.setPlayed(modInfo.getPlayed());
    modInfoBean.setPublishDate(fromPythonTime(modInfo.getDate()));
    modInfoBean.setDescription(modInfo.getDescription());
    modInfoBean.setUid(modInfo.getUid());
    modInfoBean.setDownloads(modInfo.getDownloads());
    modInfoBean.setThumbnailUrl(modInfo.getThumbnail());
    modInfoBean.getComments().setAll(modInfo.getComments());
    modInfoBean.setDownloadUrl(modInfo.getLink());
    return modInfoBean;
  }

  public ObservableList<String> getComments() {
    return comments.get();
  }

  public void setComments(ObservableList<String> comments) {
    this.comments.set(comments);
  }
}
