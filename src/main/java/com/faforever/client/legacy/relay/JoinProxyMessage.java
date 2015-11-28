package com.faforever.client.legacy.relay;

public class JoinProxyMessage extends GpgServerMessage {

  public static final int PLAYER_NR_INDEX = 0;
  public static final int USERNAME_INDEX = 2;
  public static final int PEER_UID_INDEX = 3;

  public JoinProxyMessage() {
    super(GpgServerMessageType.JOIN_PROXY, 4);
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }

  public String getUsername() {
    return getString(USERNAME_INDEX);
  }

  public int getPlayerNumber() {
    return getInt(PLAYER_NR_INDEX);
  }
}
