package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MapReviewsSummaryBean extends ReviewsSummaryBean {
  ObjectProperty<MapBean> map = new SimpleObjectProperty<>();

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
