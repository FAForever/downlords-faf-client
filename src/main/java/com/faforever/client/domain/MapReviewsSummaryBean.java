package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MapReviewsSummaryBean extends ReviewsSummaryBean {
  private final ObjectProperty<MapBean> map = new SimpleObjectProperty<>();

  public MapBean getMap() {
    return map.get();
  }

  public ObjectProperty<MapBean> mapProperty() {
    return map;
  }

  public void setMap(MapBean map) {
    this.map.set(map);
  }
}
