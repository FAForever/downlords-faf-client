package com.faforever.client.preferences;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.ObservableMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static javafx.collections.FXCollections.observableHashMap;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class UserPrefs {

  MapProperty<Integer, String> notesByPlayerId = new SimpleMapProperty<>(observableHashMap());

  public ObservableMap<Integer, String> getNotesByPlayerId() {
    return notesByPlayerId;
  }
}
