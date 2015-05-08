package com.faforever.client.game;

import com.faforever.client.legacy.domain.ModInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ModInfoBean {

  private StringProperty name;
  private StringProperty fullName;
  private StringProperty description;

  public ModInfoBean(ModInfo modInfo) {
    name = new SimpleStringProperty(modInfo.name);
    fullName = new SimpleStringProperty(modInfo.fullname);
    description = new SimpleStringProperty(modInfo.desc);
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
