package com.faforever.client.legacy.relay;

public class P2pReconnectMessage extends GpgServerMessage {

  public P2pReconnectMessage() {
    super(GpgServerMessageType.P2P_RECONNECT, 0);
  }
}
