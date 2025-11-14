package com.faforever.client.preferences;

import com.faforever.commons.lobby.Faction;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableSet;


public class MatchmakerPrefs {
  private final ObservableList<Faction> factions = observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF,
                                                                       Faction.SERAPHIM);
  private final ObservableSet<Integer> unselectedQueueIds = observableSet();

  private final ObservableMap<VetoKey, Integer> appliedVetoes = observableHashMap();

  private final ObservableSet<Integer> selectedQueueIds = observableSet();


  public ObservableList<Faction> getFactions() {
    return factions;
  }

  public ObservableSet<Integer> getUnselectedQueueIds() {
    return unselectedQueueIds;
  }

  public ObservableMap<VetoKey, Integer> getAppliedVetoes() {
    return appliedVetoes;
  }

  public ObservableSet<Integer> getSelectedQueueIds() {
    return selectedQueueIds;
  }
}
