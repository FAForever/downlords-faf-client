package com.faforever.client.legacy.relay;

public class DisconnectFromPeerMessage extends GpgServerMessage {

  public DisconnectFromPeerMessage() {
    super(GpgServerMessageType.DISCONNECT_FROM_PEER, 0);
  }
}
