package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ModBean extends AbstractEntityBean<ModBean> {

  @ToString.Include
  @EqualsAndHashCode.Include
  StringProperty displayName = new SimpleStringProperty();
  BooleanProperty recommended = new SimpleBooleanProperty();
  StringProperty author = new SimpleStringProperty();
  ObjectProperty<PlayerBean> uploader = new SimpleObjectProperty<>();
  ObjectProperty<ModReviewsSummaryBean> modReviewsSummary = new SimpleObjectProperty<>();
  ObservableList<ModVersionBean> versions = FXCollections.observableArrayList();
  ObjectProperty<ModVersionBean> latestVersion = new SimpleObjectProperty<>();

  public String getDisplayName() {
    return displayName.get();
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
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

  public ModReviewsSummaryBean getModReviewsSummary() {
    return modReviewsSummary.get();
  }

  public void setModReviewsSummary(ModReviewsSummaryBean modReviewsSummary) {
    this.modReviewsSummary.set(modReviewsSummary);
  }

  public ObjectProperty<ModReviewsSummaryBean> modReviewsSummaryProperty() {
    return modReviewsSummary;
  }

  public PlayerBean getUploader() {
    return uploader.get();
  }

  public void setUploader(PlayerBean uploader) {
    this.uploader.set(uploader);
  }

  public ObjectProperty<PlayerBean> uploaderProperty() {
    return uploader;
  }

  public ObservableList<ModVersionBean> getVersions() {
    return versions;
  }

  public void setVersions(List<ModVersionBean> versions) {
    if (versions == null) {
      versions = List.of();
    }
    this.versions.setAll(versions);
  }

  public ModVersionBean getLatestVersion() {
    return latestVersion.get();
  }

  public void setLatestVersion(ModVersionBean latestVersion) {
    this.latestVersion.set(latestVersion);
  }

  public ObjectProperty<ModVersionBean> latestVersionProperty() {
    return latestVersion;
  }

  public boolean getRecommended() {
    return recommended.get();
  }

  public BooleanProperty recommendedProperty() {
    return recommended;
  }

  public void setRecommended(boolean recommended) {
    this.recommended.set(recommended);
  }
}
