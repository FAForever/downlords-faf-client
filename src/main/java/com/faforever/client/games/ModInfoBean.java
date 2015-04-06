package com.faforever.client.games;

import com.faforever.client.legacy.ModInfoMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ModInfoBean {

  private StringProperty name;
  private StringProperty fullName;
  private StringProperty description;

  public ModInfoBean(ModInfoMessage modInfoMessage) {
    name = new SimpleStringProperty(modInfoMessage.name);
    fullName = new SimpleStringProperty(modInfoMessage.fullname);
    description = new SimpleStringProperty(modInfoMessage.desc);
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
