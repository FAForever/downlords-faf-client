package com.faforever.client.remote.domain;

public class SetPartyFactionsMessage extends ClientMessage {

  private boolean[] factions;

  public SetPartyFactionsMessage(boolean[] factions) {
    super(ClientMessageType.SET_PARTY_FACTIONS);
    this.factions = factions;
  }

  public boolean[] getFactions() {
    return factions;
  }

  public void setFactions(boolean[] factions) {
    this.factions = factions;
  }
}
