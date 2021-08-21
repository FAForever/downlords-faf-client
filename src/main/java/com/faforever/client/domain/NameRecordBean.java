package com.faforever.client.domain;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.time.OffsetDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
public class NameRecordBean {
  @EqualsAndHashCode.Include
  @ToString.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  @ToString.Include
  StringProperty name = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> changeTime = new SimpleObjectProperty<>();

  public Integer getId() {
    return id.get();
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public OffsetDateTime getChangeTime() {
    return changeTime.get();
  }

  public void setChangeTime(OffsetDateTime changeTime) {
    this.changeTime.set(changeTime);
  }

  public ObjectProperty<OffsetDateTime> changeTimeProperty() {
    return changeTime;
  }
}
