package com.faforever.client.fa.relay.ice.event;

import com.faforever.client.fa.relay.GpgGameMessage;

public class GpgGameMessageEvent {
  private final GpgGameMessage gpgGameMessage;

  public GpgGameMessageEvent(GpgGameMessage gpgGameMessage) {
    this.gpgGameMessage = gpgGameMessage;
  }

  public GpgGameMessage getGpgGameMessage() {
    return gpgGameMessage;
  }
}
