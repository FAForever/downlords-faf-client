package com.faforever.client.legacy.relay;

public class ConnectToProxyMessage extends RelayServerMessage{

  private static final int PLAYER_NUMBER_INDEX = 0;
  private static final int USERNAME_INDEX = 2;
  private static final int PEER_UID_INDEX = 3;

  public int getPlayerNumber() {
    return (int) getArgs().get(PLAYER_NUMBER_INDEX);
  }

  public String getUsername() {
    return (String) getArgs().get(USERNAME_INDEX);
  }

  public int getPeerUid() {
    return (int) getArgs().get(PEER_UID_INDEX);
  }
}
