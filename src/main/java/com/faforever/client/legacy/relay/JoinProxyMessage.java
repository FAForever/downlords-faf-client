package com.faforever.client.legacy.relay;

public class JoinProxyMessage extends RelayServerMessage{

  public static final int PLAYER_NR_INDEX = 0;
  public static final int USERNAME_INDEX = 2;
  public static final int PEER_UID_INDEX = 3;

  public int getPeerUid() {
    return asInt(commands.get(PEER_UID_INDEX));
  }

  public String getUsername() {
    return (String) commands.get(USERNAME_INDEX);
  }

  public int getPlayerNumber() {
    return asInt(commands.get(PLAYER_NR_INDEX));
  }
}
