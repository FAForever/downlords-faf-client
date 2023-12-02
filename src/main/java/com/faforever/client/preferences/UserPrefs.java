package com.faforever.client.preferences;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.ObservableMap;

import static javafx.collections.FXCollections.observableHashMap;


public class UserPrefs {

  private final MapProperty<Integer, String> notesByPlayerId = new SimpleMapProperty<>(observableHashMap());

  public ObservableMap<Integer, String> getNotesByPlayerId() {
    return notesByPlayerId.get();
  }

  public void setNotesByPlayerId(ObservableMap<Integer, String> notesByPlayerId) {
    this.notesByPlayerId.set(notesByPlayerId);
  }
}
