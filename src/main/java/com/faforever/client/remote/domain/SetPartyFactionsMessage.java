package com.faforever.client.remote.domain;

import com.faforever.client.game.Faction;

import java.util.List;
import java.util.stream.Collectors;

public class SetPartyFactionsMessage extends ClientMessage {

  private List<String> factions;

  public SetPartyFactionsMessage(List<Faction> factions) {
    super(ClientMessageType.SET_PARTY_FACTIONS);
    this.factions = factions.stream().map(Faction::getString).collect(Collectors.toList());
  }

  public List<String> getFactions() {
    return factions;
  }

  public void setFactions(List<String> factions) {
    this.factions = factions;
  }
}
