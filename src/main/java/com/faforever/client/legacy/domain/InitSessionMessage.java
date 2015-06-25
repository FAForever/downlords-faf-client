package com.faforever.client.legacy.domain;

public class InitSessionMessage extends ClientMessage {

  public InitSessionMessage() {
    command = "ask_session";
  }
}
