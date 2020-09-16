package com.faforever.client.preferences;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class BlacklistPrefs {
  private final SetProperty<String> mapBlacklistProperty;
  private final SetProperty<String> mapFilterProperty;
  private final SetProperty<String> modBlacklistProperty;

  public BlacklistPrefs() {
    mapBlacklistProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    mapFilterProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    modBlacklistProperty = new SimpleSetProperty<>(FXCollections.observableSet());
  }

  public SetProperty<String> getMapBlacklistProperty() {
    return mapBlacklistProperty;
  }

  public SetProperty<String> getMapFilterProperty() {
    return mapFilterProperty;
  }

  public ObservableSet<String> getModBlacklistProperty() {
    return modBlacklistProperty.get();
  }
}
