package com.faforever.client.remote.domain;

public final class PingMessage extends ClientMessage {

  public static final PingMessage INSTANCE = new PingMessage();

  private PingMessage() {
    super(ClientMessageType.PING);
  }
}
