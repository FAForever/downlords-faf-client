package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ModBean extends AbstractEntityBean {

  @ToString.Include
  @EqualsAndHashCode.Include
  private final StringProperty displayName = new SimpleStringProperty();
  private final BooleanProperty recommended = new SimpleBooleanProperty();
  private final StringProperty author = new SimpleStringProperty();
  private final ObjectProperty<PlayerBean> uploader = new SimpleObjectProperty<>();
  private final ObjectProperty<ModReviewsSummaryBean> modReviewsSummary = new SimpleObjectProperty<>();
  private final ObjectProperty<ModVersionBean> latestVersion = new SimpleObjectProperty<>();

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
