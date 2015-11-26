package com.faforever.client.legacy.relay;

public class ConnectToProxyMessage extends GpgServerMessage {

  private static final int PLAYER_NUMBER_INDEX = 0;
  private static final int USERNAME_INDEX = 2;
  private static final int PEER_UID_INDEX = 3;

  public int getPlayerNumber() {
    return getInt(PLAYER_NUMBER_INDEX);
  }

  public String getUsername() {
    return (String) getArgs().get(USERNAME_INDEX);
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }
}
