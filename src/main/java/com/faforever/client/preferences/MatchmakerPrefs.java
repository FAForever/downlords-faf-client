package com.faforever.client.preferences;

import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.VetoData;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.List;
import java.util.stream.Collectors;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableSet;

public class MatchmakerPrefs {
  private final ObservableList<Faction> factions = observableArrayList(Faction.AEON, Faction.CYBRAN, Faction.UEF,
                                                                       Faction.SERAPHIM);
  private final ObservableSet<Integer> unselectedQueueIds = observableSet();

  private final ObservableMap<VetoKey, Integer> appliedVetoes = observableHashMap();

  public ObservableList<Faction> getFactions() {
    return factions;
  }

  public ObservableSet<Integer> getUnselectedQueueIds() {
    return unselectedQueueIds;
  }

  public ObservableMap<VetoKey, Integer> getAppliedVetoes() {
    return appliedVetoes;
  }

  public void setTokensForMap(VetoKey vetoKey, Integer vetoTokensApplied) {
    appliedVetoes.put(vetoKey, vetoTokensApplied);
  }

  public void setAllVetoes(List<VetoData> vetoes) {
    appliedVetoes.clear();
    vetoes.forEach(v -> appliedVetoes.put(
        new VetoKey(v.getMatchmakerQueueMapPoolId(), v.getMapPoolMapVersionId()),
        v.getVetoTokensApplied()
    ));
  }

  public List<VetoData> getVetoesAsList() {
    return appliedVetoes.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() > 0)
                        .map(entry -> new VetoData(
                            entry.getKey().mapPoolMapVersionId(),
                            entry.getValue(),
                            entry.getKey().matchmakerQueueMapPoolId()))
                        .collect(Collectors.toList());
  }

}
