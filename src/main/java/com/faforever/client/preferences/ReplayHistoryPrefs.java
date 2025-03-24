package com.faforever.client.preferences;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class ReplayHistoryPrefs {

  private final SetProperty<Integer> watchedReplayMap = new SimpleSetProperty<>(
      FXCollections.observableSet());

  public ObservableSet<Integer> getWatchedReplayMap() {return watchedReplayMap.getValue();}

  public void setWatchedReplayMap(ObservableSet<Integer> watchedReplayMap) {
    this.watchedReplayMap.setValue(watchedReplayMap);
  }

  public SetProperty<Integer> watchedReplayMapProperty() {return watchedReplayMap;}

}
