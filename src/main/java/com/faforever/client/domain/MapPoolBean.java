package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class MapPoolBean extends AbstractEntityBean<MapPoolBean> {
  StringProperty name = new SimpleStringProperty();
  ObjectProperty<MatchmakerQueueMapPoolBean> mapPool = new SimpleObjectProperty<>();
  ObservableList<MapPoolAssignmentBean> poolAssignments = FXCollections.emptyObservableList();

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public MatchmakerQueueMapPoolBean getMapPool() {
    return mapPool.get();
  }

  public ObjectProperty<MatchmakerQueueMapPoolBean> mapPoolProperty() {
    return mapPool;
  }

  public void setMapPool(MatchmakerQueueMapPoolBean mapPool) {
    this.mapPool.set(mapPool);
  }

  public ObservableList<MapPoolAssignmentBean> getPoolAssignments() {
    return poolAssignments;
  }

  public void setPoolAssignments(List<MapPoolAssignmentBean> poolAssignments) {
    if (poolAssignments == null) {
      poolAssignments = List.of();
    }
    this.poolAssignments.setAll(poolAssignments);
  }
}
