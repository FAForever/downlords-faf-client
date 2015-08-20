package com.faforever.client.legacy.domain;

public class InitSessionMessage extends ClientMessage {

  public InitSessionMessage() {
    setCommand(ClientMessageType.ASK_SESSION);
  }
}
