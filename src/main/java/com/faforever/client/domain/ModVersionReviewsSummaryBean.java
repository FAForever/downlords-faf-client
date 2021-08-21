package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ModVersionReviewsSummaryBean extends ReviewsSummaryBean {
  ObjectProperty<ModVersionBean> modVersion = new SimpleObjectProperty<>();

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
