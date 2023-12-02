package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;


public class GeneralPrefs {

  private final BooleanProperty disableSteamStart = new SimpleBooleanProperty(false);
  private final BooleanProperty showCyrillicWarning = new SimpleBooleanProperty(true);

  public boolean getDisableSteamStart() {
    return disableSteamStart.get();
  }

  public BooleanProperty disableSteamStartProperty() {
    return disableSteamStart;
  }

  public void setDisableSteamStart(boolean disableSteamStart) {
    this.disableSteamStart.set(disableSteamStart);
  }

  public boolean isShowCyrillicWarning() {
    return showCyrillicWarning.get();
  }

  public BooleanProperty showCyrillicWarningProperty() {
    return showCyrillicWarning;
  }

  public void setShowCyrillicWarning(boolean showCyrillicWarning) {
    this.showCyrillicWarning.set(showCyrillicWarning);
  }
}
