package com.faforever.client.preferences;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class ReplayHistoryPrefs {

  private final SetProperty<Integer> watchedReplays = new SimpleSetProperty<>(
      FXCollections.observableSet());

  public ObservableSet<Integer> getWatchedReplays() {return watchedReplays.getValue();}

  public void setWatchedReplays(ObservableSet<Integer> watchedReplays) {
    this.watchedReplays.setValue(watchedReplays);
  }

  public SetProperty<Integer> watchedReplaysProperty() {return watchedReplays;}

}
