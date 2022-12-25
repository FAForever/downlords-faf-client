package com.faforever.client.domain;

import com.faforever.commons.api.dto.CoopFaction;
import com.faforever.commons.api.dto.CoopType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CoopScenarioBean {

  @EqualsAndHashCode.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  StringProperty name = new SimpleStringProperty();
  StringProperty description = new SimpleStringProperty();
  IntegerProperty order = new SimpleIntegerProperty();
  ObjectProperty<CoopType> type = new SimpleObjectProperty<>();
  ObjectProperty<CoopFaction> faction = new SimpleObjectProperty<>();
  ObservableList<CoopMissionBean> maps = FXCollections.observableArrayList();

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public Integer getOrder() {
    return order.get();
  }

  public void setOrder(Integer order) {
    this.order.set(order);
  }

  public IntegerProperty orderProperty() {
    return order;
  }

  public ObservableList<CoopMissionBean> getMaps() {
    return maps;
  }

  public void setMaps(List<CoopMissionBean> maps) {
    this.maps.setAll(maps != null ? maps : Collections.emptyList());
  }

  public void setType(CoopType type) {
    this.type.set(type);
  }

  public CoopType getType() {
    return type.get();
  }

  public ObjectProperty<CoopType> typeProperty() {
    return type;
  }

  public void setFaction(CoopFaction faction) {
    this.faction.set(faction);
  }

  public CoopFaction getFaction() {
    return faction.get();
  }

  public ObjectProperty<CoopFaction> factionProperty() {
    return faction;
  }
}
