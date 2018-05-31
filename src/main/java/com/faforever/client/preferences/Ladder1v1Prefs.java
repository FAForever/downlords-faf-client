package com.faforever.client.preferences;

import com.faforever.client.game.Faction;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ObservableList;

import static javafx.collections.FXCollections.observableArrayList;

public class Ladder1v1Prefs {

  private final ListProperty<Faction> factions;

  public Ladder1v1Prefs() {
    this.factions = new SimpleListProperty<>(observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF, Faction.SERAPHIM));
  }

  public ObservableList<Faction> getFactions() {
    return factions.get();
  }

  public ListProperty<Faction> factionsProperty() {
    return factions;
  }

}
