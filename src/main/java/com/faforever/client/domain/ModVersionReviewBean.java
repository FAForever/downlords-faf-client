package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class ModVersionReviewBean extends ReviewBean {
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

  @Override
  public ComparableVersion getVersion() {
    return getModVersion().getVersion();
  }

  @Override
  public ComparableVersion getLatestVersion() {
    return getModVersion().getMod().getLatestVersion().getVersion();
  }
}
