package com.faforever.client.legacy.domain;

public class InitSessionMessage extends ClientMessage {

  public InitSessionMessage() {
    super(ClientMessageType.ASK_SESSION);
  }
}
