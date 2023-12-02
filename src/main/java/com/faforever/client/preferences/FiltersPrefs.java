package com.faforever.client.preferences;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class FiltersPrefs {

  private final ListProperty<String> mapNameBlacklist = new SimpleListProperty<>(FXCollections.observableArrayList());

  public ObservableList<String> getMapNameBlacklist() {
    return mapNameBlacklist.getValue();
  }

  public void setMapNameBlacklist(ObservableList<String> mapNameBlacklist) {
    this.mapNameBlacklist.setValue(mapNameBlacklist);
  }

  public ListProperty<String> mapNameBlacklistProperty() {
    return mapNameBlacklist;
  }
}
