package com.faforever.client.preferences;

import com.faforever.commons.lobby.Faction;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableSet;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MatchmakerPrefs {

  ObservableList<Faction> factions = observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM);
  ObservableSet<Integer> unselectedQueueIds = observableSet();

  public ObservableList<Faction> getFactions() {
    return factions;
  }

  public ObservableSet<Integer> getUnselectedQueueIds() {
    return unselectedQueueIds;
  }
}
