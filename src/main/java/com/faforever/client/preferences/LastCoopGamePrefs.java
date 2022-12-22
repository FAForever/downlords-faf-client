package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class LastCoopGamePrefs {

  StringProperty lastTitle = new SimpleStringProperty();
  StringProperty lastPassword = new SimpleStringProperty();
  StringProperty lastMapFolderName = new SimpleStringProperty();
  BooleanProperty offlineMode = new SimpleBooleanProperty(false);

  public String getLastTitle() {
    return lastTitle.get();
  }

  public void setLastTitle(String lastTitle) {
    this.lastTitle.set(lastTitle);
  }

  public StringProperty lastTitleProperty() {
    return lastTitle;
  }

  public String getLastPassword() {
    return lastPassword.get();
  }

  public void setLastPassword(String lastPassword) {
    this.lastPassword.set(lastPassword);
  }

  public StringProperty lastPasswordProperty() {
    return lastPassword;
  }

  public String getLastMapFolderName() {
    return lastMapFolderName.get();
  }

  public void setLastMapFolderName(String lastMapFolderName) {
    this.lastMapFolderName.set(lastMapFolderName);
  }

  public StringProperty lastMapFolderName() {
    return lastMapFolderName;
  }

  public boolean isOfflineMode() {
    return offlineMode.get();
  }

  public void setOfflineMode(boolean offlineMode) {
    this.offlineMode.set(offlineMode);
  }

  public BooleanProperty offlineModeProperty() {
    return offlineMode;
  }
}
