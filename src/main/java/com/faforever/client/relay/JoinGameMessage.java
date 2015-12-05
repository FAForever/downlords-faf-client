package com.faforever.client.relay;

import com.faforever.client.util.SocketAddressUtil;

import java.net.InetSocketAddress;

public class JoinGameMessage extends GpgServerMessage {

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public JoinGameMessage() {
    super(GpgServerMessageType.JOIN_GAME, 3);
  }

  public InetSocketAddress getPeerAddress() {
    return SocketAddressUtil.fromString(getString(PEER_ADDRESS_INDEX));
  }

  public void setPeerAddress(InetSocketAddress peerAddress) {
    setValue(PEER_ADDRESS_INDEX, SocketAddressUtil.toString(peerAddress));
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }

  public void setPeerUid(int peerUid) {
    setValue(PEER_UID_INDEX, peerUid);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
  }
}
