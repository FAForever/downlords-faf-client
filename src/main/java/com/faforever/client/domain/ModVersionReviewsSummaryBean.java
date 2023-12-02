package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ModVersionReviewsSummaryBean extends ReviewsSummaryBean {
  private final ObjectProperty<ModVersionBean> modVersion = new SimpleObjectProperty<>();

  public ModVersionBean getModVersion() {
    return modVersion.get();
  }

  public ObjectProperty<ModVersionBean> modVersionProperty() {
    return modVersion;
  }

  public void setModVersion(ModVersionBean modVersion) {
    this.modVersion.set(modVersion);
  }
}
