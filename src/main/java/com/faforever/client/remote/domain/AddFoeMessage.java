package com.faforever.client.remote.domain;

public class AddFoeMessage extends ClientMessage {

  private int foe;

  public AddFoeMessage(int playerId) {
    super(ClientMessageType.SOCIAL_ADD);
    this.foe = playerId;
  }

  public int getFoe() {
    return foe;
  }
}
