package com.faforever.client.teammatchmaking;

import com.faforever.client.player.Player;
import com.faforever.commons.api.dto.Faction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class Party {

  private final ObjectProperty<Player> owner;
  private final ObservableList<PartyMember> members;

  public Party() {
    owner = new SimpleObjectProperty<>();
    members = FXCollections.observableArrayList();
  }

  public Player getOwner() {
    return owner.get();
  }

  public void setOwner(Player owner) {
    this.owner.set(owner);
  }

  public ObjectProperty<Player> ownerProperty() {
    return owner;
  }

  public ObservableList<PartyMember> getMembers() {
    return members;
  }

  public void setMembers(ObservableList<PartyMember> members) {
    this.members.setAll(members);
  }

  @Data
  @AllArgsConstructor
  public static class PartyMember {
    private final Player player;
    private List<Faction> factions;

    public PartyMember(Player player) {
      this.player = player;
      this.factions = Arrays.asList(Faction.AEON, Faction.CYBRAN, Faction.SERAPHIM, Faction.UEF);
    }
  }
}
