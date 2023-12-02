package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
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
  public ObservableValue<ComparableVersion> versionProperty() {
    return modVersion.flatMap(ModVersionBean::versionProperty);
  }

  @Override
  public ObservableValue<ComparableVersion> latestVersionProperty() {
    return modVersion.flatMap(ModVersionBean::modProperty)
        .flatMap(ModBean::latestVersionProperty)
        .flatMap(ModVersionBean::versionProperty);
  }
}
