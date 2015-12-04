package com.faforever.client.relay;

public class HostGameMessage extends GpgServerMessage {

  public HostGameMessage() {
    super(GpgServerMessageType.HOST_GAME, 0);
  }
}
