package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
  public ComparableVersion getVersion() {
    return getMapVersion().getVersion();
  }

  @Override
  public ComparableVersion getLatestVersion() {
    return getMapVersion().getMap().getLatestVersion().getVersion();
  }
}
