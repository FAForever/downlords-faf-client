package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class MapVersionReviewBean extends ReviewBean {
  ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();

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
    return mapVersion.flatMap(MapVersionBean::versionProperty);
  }

  @Override
  public ObservableValue<ComparableVersion> latestVersionProperty() {
    return mapVersion.flatMap(MapVersionBean::mapProperty)
        .flatMap(MapBean::latestVersionProperty)
        .flatMap(MapVersionBean::versionProperty);
  }
}
