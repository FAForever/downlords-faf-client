package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URL;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class LeagueBean extends AbstractEntityBean {
  @ToString.Include
  private final StringProperty technicalName = new SimpleStringProperty();
  private final BooleanProperty enabled = new SimpleBooleanProperty();
  private final StringProperty nameKey = new SimpleStringProperty();
  private final StringProperty descriptionKey = new SimpleStringProperty();
  private final ObjectProperty<URL> imageUrl = new SimpleObjectProperty<>();
  private final ObjectProperty<URL> mediumImageUrl = new SimpleObjectProperty<>();
  private final ObjectProperty<URL> smallImageUrl = new SimpleObjectProperty<>();

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
