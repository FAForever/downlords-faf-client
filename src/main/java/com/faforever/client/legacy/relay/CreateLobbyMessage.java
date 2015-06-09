package com.faforever.client.legacy.relay;

public class CreateLobbyMessage extends RelayServerMessage {

  public static final int PORT_INDEX = 1;
  public static final int PEER_UID_INDEX = 3;

  public int getPeerUid() {
    return asInt(getArgs().get(PEER_UID_INDEX));
  }

  public void setPort(int port) {
    getArgs().set(PORT_INDEX, port);
  }
}
