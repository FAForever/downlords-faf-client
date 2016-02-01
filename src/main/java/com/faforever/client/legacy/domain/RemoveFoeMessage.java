package com.faforever.client.legacy.domain;

public class RemoveFoeMessage extends ClientMessage {

  private int foeId;

  public RemoveFoeMessage(int foeId) {
    super(ClientMessageType.SOCIAL_REMOVE);
    this.foeId = foeId;
  }

  public int getFoeId() {
    return foeId;
  }
}
