package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class FiltersPrefs {

  private final ListProperty<String> mapNameBlacklist = new SimpleListProperty<>(FXCollections.observableArrayList());
  private final BooleanProperty mapNameBlacklistEnabled = new SimpleBooleanProperty(false);

  public ObservableList<String> getMapNameBlacklist() {
    return mapNameBlacklist.getValue();
  }

  public void setMapNameBlacklist(ObservableList<String> mapNameBlacklist) {
    this.mapNameBlacklist.setValue(mapNameBlacklist);
  }

  public ListProperty<String> mapNameBlacklistProperty() {
    return mapNameBlacklist;
  }

  public boolean isMapNameBlacklistEnabled() {
    return mapNameBlacklistEnabled.get();
  }

  public void setMapNameBlacklistEnabled(boolean mapNameBlacklistEnabled) {
    this.mapNameBlacklistEnabled.set(mapNameBlacklistEnabled);
  }

  public BooleanProperty mapNameBlacklistEnabledProperty() {
    return mapNameBlacklistEnabled;
  }
}
