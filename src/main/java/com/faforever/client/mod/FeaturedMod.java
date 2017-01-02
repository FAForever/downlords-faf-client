package com.faforever.client.mod;

import com.google.common.base.Strings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FeaturedMod {

  public static final FeaturedMod UNKNOWN = new FeaturedMod();
  private final StringProperty id;
  private final StringProperty technicalName;
  private final StringProperty displayName;
  private final StringProperty description;
  private final StringProperty gitUrl;
  private final StringProperty gitBranch;
  private final BooleanProperty visible;

  public FeaturedMod() {
    id = new SimpleStringProperty();
    technicalName = new SimpleStringProperty();
    displayName = new SimpleStringProperty();
    description = new SimpleStringProperty();
    visible = new SimpleBooleanProperty();
    gitUrl = new SimpleStringProperty();
    gitBranch = new SimpleStringProperty();
  }

  public static FeaturedMod fromFeaturedMod(com.faforever.client.api.dto.FeaturedMod featuredMod) {
    FeaturedMod bean = new FeaturedMod();
    bean.setId(featuredMod.getId());
    bean.technicalName.set(featuredMod.getTechnicalName());
    bean.displayName.set(featuredMod.getDisplayName());
    bean.description.setValue(featuredMod.getDescription());
    bean.visible.setValue(featuredMod.isVisible());
    bean.gitUrl.set(Strings.emptyToNull(featuredMod.getGitUrl()));
    bean.gitBranch.set(Strings.emptyToNull(featuredMod.getGitBranch()));
    return bean;
  }

  public String getTechnicalName() {
    return technicalName.get();
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName.set(technicalName);
  }

  public StringProperty technicalNameProperty() {
    return technicalName;
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
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

  public boolean isVisible() {
    return visible.get();
  }

  public void setVisible(boolean visible) {
    this.visible.set(visible);
  }

  public BooleanProperty visibleProperty() {
    return visible;
  }

  public String getGitUrl() {
    return gitUrl.get();
  }

  public void setGitUrl(String gitUrl) {
    this.gitUrl.set(gitUrl);
  }

  public StringProperty gitUrlProperty() {
    return gitUrl;
  }

  public String getGitBranch() {
    return gitBranch.get();
  }

  public void setGitBranch(String gitBranch) {
    this.gitBranch.set(gitBranch);
  }

  public StringProperty gitBranchProperty() {
    return gitBranch;
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
}
