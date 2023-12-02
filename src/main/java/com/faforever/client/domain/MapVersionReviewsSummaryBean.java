package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MapVersionReviewsSummaryBean extends ReviewsSummaryBean {
  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();

  public MapVersionBean getMapVersion() {
    return mapVersion.get();
  }

  public ObjectProperty<MapVersionBean> mapVersionProperty() {
    return mapVersion;
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }
}
