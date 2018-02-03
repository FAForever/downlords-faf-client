package com.faforever.client.remote.domain;

public class ListIceServersMessage extends ClientMessage {

  public ListIceServersMessage() {
    super(ClientMessageType.ICE_SERVERS);
  }
}
