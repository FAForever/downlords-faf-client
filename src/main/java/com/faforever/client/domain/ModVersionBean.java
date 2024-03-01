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
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ModVersionBean extends AbstractEntityBean {
  private final ObjectProperty<Path> imagePath = new SimpleObjectProperty<>();

  /**
   * UID as specified in the mod itself (specified by the uploader).
   */
  @ToString.Include
  @EqualsAndHashCode.Include
  private final StringProperty uid = new SimpleStringProperty();
  private final StringProperty description = new SimpleStringProperty();
  private final BooleanProperty selectable = new SimpleBooleanProperty();
  private final ObjectProperty<ComparableVersion> version = new SimpleObjectProperty<>();
  private final ObjectProperty<URL> thumbnailUrl = new SimpleObjectProperty<>();
  private final ObservableList<String> comments = FXCollections.observableArrayList();
  private final BooleanProperty selected = new SimpleBooleanProperty();
  private final IntegerProperty played = new SimpleIntegerProperty();
  private final ObjectProperty<URL> downloadUrl = new SimpleObjectProperty<>();
  private final ObjectProperty<ModVersionReviewsSummaryBean> reviewsSummary = new SimpleObjectProperty<>();
  private final ObjectProperty<ModType> modType = new SimpleObjectProperty<>();
  private final StringProperty filename = new SimpleStringProperty();
  private final StringProperty icon = new SimpleStringProperty();
  private final BooleanProperty ranked = new SimpleBooleanProperty();
  private final BooleanProperty hidden = new SimpleBooleanProperty();
  private final ObjectProperty<ModBean> mod = new SimpleObjectProperty<>();

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

  public int getPlayed() {
    return played.get();
  }

  public void setPlayed(int played) {
    this.played.set(played);
  }

  public IntegerProperty playedProperty() {
    return played;
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

  public ObservableList<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    if (comments == null) {
      comments = List.of();
    }
    this.comments.setAll(comments);
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

  public ModVersionReviewsSummaryBean getReviewsSummary() {
    return reviewsSummary.get();
  }

  public void setReviewsSummary(ModVersionReviewsSummaryBean reviewsSummary) {
    this.reviewsSummary.set(reviewsSummary);
  }

  public ObjectProperty<ModVersionReviewsSummaryBean> reviewsSummaryProperty() {
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

  public boolean getRanked() {
    return ranked.get();
  }

  public void setRanked(boolean ranked) {
    this.ranked.set(ranked);
  }

  public BooleanProperty rankedProperty() {
    return ranked;
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

  public ModBean getMod() {
    return mod.get();
  }

  public void setMod(ModBean mod) {
    this.mod.set(mod);
  }

  public ObjectProperty<ModBean> modProperty() {
    return mod;
  }

  @RequiredArgsConstructor
  public enum ModType {
    UI("modType.ui"),
    SIM("modType.sim");

    @Getter
    private final String i18nKey;
  }
}
