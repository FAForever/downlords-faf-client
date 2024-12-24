package com.faforever.client.preferences;

import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.VetoData;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.IntStream;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableSet;

@Slf4j
public class MatchmakerPrefs {
  private final ObservableList<Faction> factions = observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF,
                                                                       Faction.SERAPHIM);
  private final ObservableSet<Integer> unselectedQueueIds = observableSet();

  private final ObservableList<VetoData> appliedVetoes = observableArrayList();

  public ObservableList<Faction> getFactions() {
    return factions;
  }

  public ObservableSet<Integer> getUnselectedQueueIds() {
    return unselectedQueueIds;
  }

  public ObservableList<VetoData> getAppliedVetoes() {
    return appliedVetoes;
  }

  public void setVetoData(VetoData vetoData) {
    int index = IntStream.range(0, appliedVetoes.size())
        .filter(i -> appliedVetoes.get(i).getMapPoolMapVersionId() == vetoData.getMapPoolMapVersionId())
        .findFirst()
        .orElse(-1);
    if (index == -1) {
      appliedVetoes.add(vetoData);
    } else {
      appliedVetoes.set(index, vetoData);
    }
    log.debug("Vetoes: " + appliedVetoes);
  }
}
