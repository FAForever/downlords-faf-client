package com.faforever.client.domain;

import com.faforever.commons.api.dto.MapParams;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MapPoolAssignmentBean extends AbstractEntityBean {
  private final ObjectProperty<MapParams> mapParams = new SimpleObjectProperty<>();
  private final ObjectProperty<MapPoolBean> mapPool = new SimpleObjectProperty<>();
  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  private final IntegerProperty weight = new SimpleIntegerProperty();

  public MapParams getMapParams() {
    return mapParams.get();
  }

  public ObjectProperty<MapParams> mapParamsProperty() {
    return mapParams;
  }

  public void setMapParams(MapParams mapParams) {
    this.mapParams.set(mapParams);
  }

  public MapPoolBean getMapPool() {
    return mapPool.get();
  }

  public ObjectProperty<MapPoolBean> mapPoolProperty() {
    return mapPool;
  }

  public void setMapPool(MapPoolBean mapPool) {
    this.mapPool.set(mapPool);
  }

  public MapVersionBean getMapVersion() {
    return mapVersion.get();
  }

  public ObjectProperty<MapVersionBean> mapVersionProperty() {
    return mapVersion;
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  public int getWeight() {
    return weight.get();
  }

  public IntegerProperty weightProperty() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight.set(weight);
  }
}
