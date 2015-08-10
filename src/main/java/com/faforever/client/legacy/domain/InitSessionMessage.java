package com.faforever.client.legacy.domain;

public class InitSessionMessage extends ClientMessage {

  public InitSessionMessage() {
    setCommand("ask_session");
  }
}
