package com.faforever.client.remote.domain;

public class PingMessage extends ClientMessage {

  public static final PingMessage INSTANCE = new PingMessage();

  private PingMessage() {
    super(ClientMessageType.PING);
  }
}
