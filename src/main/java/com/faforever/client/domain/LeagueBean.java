package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.net.URL;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LeagueBean extends AbstractEntityBean<LeagueBean> {
  @ToString.Include
  StringProperty technicalName = new SimpleStringProperty();
  BooleanProperty enabled = new SimpleBooleanProperty();
  StringProperty nameKey = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();
  ObjectProperty<URL> imageUrl = new SimpleObjectProperty<>();
  ObjectProperty<URL> mediumImageUrl = new SimpleObjectProperty<>();
  ObjectProperty<URL> smallImageUrl = new SimpleObjectProperty<>();

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

  public boolean getEnabled() {
    return enabled.get();
  }

  public void setEnabled(boolean enabled) {
    this.enabled.set(enabled);
  }

  public BooleanProperty enabledProperty() {
    return enabled;
  }
}
