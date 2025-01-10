package com.faforever.client.preferences;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.time.OffsetDateTime;

public class ReplayHistoryPrefs {

  private final MapProperty<Integer, OffsetDateTime> watchedReplayMap = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  public ObservableMap<Integer, OffsetDateTime> getWatchedReplayMap() {return watchedReplayMap.getValue();}

  public void setWatchedReplayMap(ObservableMap<Integer, OffsetDateTime> watchedReplayMap) {
    this.watchedReplayMap.setValue(watchedReplayMap);
  }

  public MapProperty<Integer, OffsetDateTime> watchedReplayMapProperty() {return watchedReplayMap;}

}
