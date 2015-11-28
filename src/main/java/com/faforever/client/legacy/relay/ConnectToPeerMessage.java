package com.faforever.client.legacy.relay;

import static com.faforever.client.legacy.relay.GpgServerMessageType.CONNECT_TO_PEER;

public class ConnectToPeerMessage extends GpgServerMessage {

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public ConnectToPeerMessage() {
    super(CONNECT_TO_PEER, 3);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
  }

  public String getPeerAddress() {
    return getString(PEER_ADDRESS_INDEX);
  }

  public void setPeerAddress(String peerAddress) {
    setValue(PEER_ADDRESS_INDEX, peerAddress);
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }

  public void setPeerUid(int uid) {
    setValue(PEER_UID_INDEX, uid);
  }
}
