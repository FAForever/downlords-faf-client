package com.faforever.client.legacy.relay;

public class HostGameMessage extends GpgServerMessage {

  public HostGameMessage() {
    super(GpgServerMessageType.HOST_GAME, 0);
  }
}
