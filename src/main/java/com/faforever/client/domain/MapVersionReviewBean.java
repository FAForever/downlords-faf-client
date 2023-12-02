package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MapVersionReviewBean extends ReviewBean {
  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  private final ObservableValue<ComparableVersion> latestVersion = mapVersion.flatMap(MapVersionBean::mapProperty)
                                                                             .flatMap(MapBean::latestVersionProperty)
                                                                             .flatMap(MapVersionBean::versionProperty);
  private final ObservableValue<ComparableVersion> version = mapVersion.flatMap(MapVersionBean::versionProperty);

  public MapVersionBean getMapVersion() {
    return mapVersion.get();
  }

  public ObjectProperty<MapVersionBean> mapVersionProperty() {
    return mapVersion;
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
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
