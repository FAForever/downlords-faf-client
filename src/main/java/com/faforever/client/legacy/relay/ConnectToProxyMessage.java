package com.faforever.client.legacy.relay;

public class ConnectToProxyMessage extends RelayServerMessage{

  private static final int PLAYER_NUMBER_INDEX = 0;
  private static final int USERNAME_INDEX = 2;
  private static final int PEER_UID_INDEX = 3;

  public int getPlayerNumber() {
    // Keep in mind that JSON doesn't know int but only double; therefore GSON deserializes it as Double
    return ((Double) getArgs().get(PLAYER_NUMBER_INDEX)).intValue();
  }

  public String getUsername() {
    return (String) getArgs().get(USERNAME_INDEX);
  }

  public int getPeerUid() {
    // Keep in mind that JSON doesn't know int but only double; therefore GSON deserializes it as Double
    return ((Double) getArgs().get(PEER_UID_INDEX)).intValue();
  }
}
