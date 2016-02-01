package com.faforever.client.legacy.domain;

public class AddFoeMessage extends ClientMessage {

  private int foeId;

  public AddFoeMessage(int foeId) {
    super(ClientMessageType.SOCIAL_ADD);
    this.foeId = foeId;
  }

  public int getFoeId() {
    return foeId;
  }
}
