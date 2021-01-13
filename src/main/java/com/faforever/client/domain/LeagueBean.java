package com.faforever.client.domain;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
public class LeagueBean extends AbstractEntityBean<LeagueBean> {
  @ToString.Include
  StringProperty technicalName = new SimpleStringProperty();
  StringProperty nameKey = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();

  public String getDescriptionKey() {
    return descriptionKey.get();
  }

  public void setDescriptionKey(String descriptionKey) {
    this.descriptionKey.set(descriptionKey);
  }

  public StringProperty descriptionKeyProperty() {
    return descriptionKey;
  }

  public String getNameKey() {
    return nameKey.get();
  }

  public void setNameKey(String nameKey) {
    this.nameKey.set(nameKey);
  }

  public StringProperty nameKeyProperty() {
    return nameKey;
  }

  public String getTechnicalName() {
    return technicalName.get();
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName.set(technicalName);
  }

  public StringProperty technicalNameProperty() {
    return technicalName;
  }
}
