package com.faforever.client.remote.domain;

public class RemoveFoeMessage extends ClientMessage {

  private int foe;

  public RemoveFoeMessage(int playerId) {
    super(ClientMessageType.SOCIAL_REMOVE);
    this.foe = playerId;
  }

  public int getFoe() {
    return foe;
  }
}
