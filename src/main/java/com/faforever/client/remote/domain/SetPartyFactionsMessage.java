package com.faforever.client.remote.domain;

import com.faforever.client.game.Faction;

import java.util.List;
import java.util.stream.Collectors;

public class SetPartyFactionsMessage extends ClientMessage {

  private List<Integer> factions;

  public SetPartyFactionsMessage(List<Faction> factions) {
    super(ClientMessageType.SET_PARTY_FACTIONS);
    this.factions = factions.stream().map(Faction::toFaValue).collect(Collectors.toList());
  }

  public List<Integer> getFactions() {
    return factions;
  }

  public void setFactions(List<Integer> factions) {
    this.factions = factions;
  }
}
