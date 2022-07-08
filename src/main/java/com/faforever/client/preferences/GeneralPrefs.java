package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class GeneralPrefs {

  BooleanProperty disableSteamStart = new SimpleBooleanProperty(false);

  public boolean getDisableSteamStart() {
    return disableSteamStart.get();
  }

  public BooleanProperty disableSteamStartProperty() {
    return disableSteamStart;
  }

  public void setDisableSteamStart(boolean disableSteamStart) {
    this.disableSteamStart.set(disableSteamStart);
  }
}
