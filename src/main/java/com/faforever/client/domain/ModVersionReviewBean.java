package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ModVersionReviewBean extends ReviewBean {
  private final ObjectProperty<ModVersionBean> modVersion = new SimpleObjectProperty<>();
  private final ObservableValue<ComparableVersion> latestVersion = modVersion.flatMap(ModVersionBean::modProperty)
                                                                             .flatMap(ModBean::latestVersionProperty)
                                                                             .flatMap(ModVersionBean::versionProperty);
  private final ObservableValue<ComparableVersion> version = modVersion.flatMap(ModVersionBean::versionProperty);

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
  public ObservableValue<ComparableVersion> versionProperty() {
    return version;
  }

  @Override
  public ObservableValue<ComparableVersion> latestVersionProperty() {
    return latestVersion;
  }
}
