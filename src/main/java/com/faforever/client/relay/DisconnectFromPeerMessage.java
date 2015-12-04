package com.faforever.client.relay;

public class DisconnectFromPeerMessage extends GpgServerMessage {

  public DisconnectFromPeerMessage() {
    super(GpgServerMessageType.DISCONNECT_FROM_PEER, 0);
  }
}
