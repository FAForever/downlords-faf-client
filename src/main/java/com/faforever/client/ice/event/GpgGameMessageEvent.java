package com.faforever.client.ice.event;

import com.faforever.client.relay.GpgGameMessage;

public class GpgGameMessageEvent {
  private final GpgGameMessage gpgGameMessage;

  public GpgGameMessageEvent(GpgGameMessage gpgGameMessage) {
    this.gpgGameMessage = gpgGameMessage;
  }

  public GpgGameMessage getGpgGameMessage() {
    return gpgGameMessage;
  }
}
