package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameTypeInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GameTypeBean {

  private final StringProperty name;
  private final StringProperty fullName;
  private final StringProperty description;

  public GameTypeBean(GameTypeInfo gameTypeInfo) {
    name = new SimpleStringProperty(gameTypeInfo.getName());
    fullName = new SimpleStringProperty(gameTypeInfo.getFullname());
    description = new SimpleStringProperty(gameTypeInfo.getDesc());
  }

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getFullName() {
    return fullName.get();
  }

  public StringProperty fullNameProperty() {
    return fullName;
  }

  public String getDescription() {
    return description.get();
  }

  public StringProperty descriptionProperty() {
    return description;
  }
}
