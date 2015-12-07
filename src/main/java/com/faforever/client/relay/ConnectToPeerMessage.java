package com.faforever.client.relay;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.util.SocketAddressUtil;

import java.net.InetSocketAddress;

import static com.faforever.client.relay.GpgServerMessageType.CONNECT_TO_PEER;

public class ConnectToPeerMessage extends GpgServerMessage {

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public ConnectToPeerMessage() {
    super(CONNECT_TO_PEER, 3);
    setTarget(MessageTarget.GAME);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
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

  public void setPeerUid(int uid) {
    setValue(PEER_UID_INDEX, uid);
  }
}
