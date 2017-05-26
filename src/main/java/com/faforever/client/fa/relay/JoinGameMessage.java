package com.faforever.client.fa.relay;

import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.remote.domain.MessageTarget;

import java.net.InetSocketAddress;

import static com.github.nocatch.NoCatch.noCatch;

public class JoinGameMessage extends GpgServerMessage implements Cloneable {

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public JoinGameMessage() {
    super(GpgServerMessageType.JOIN_GAME, 3);
    setTarget(MessageTarget.GAME);
  }

  @Override
  public JoinGameMessage clone() {
    noCatch(() -> super.clone());
    JoinGameMessage joinGameMessage = new JoinGameMessage();
    joinGameMessage.setPeerAddress(getPeerAddress());
    joinGameMessage.setUsername(getUsername());
    joinGameMessage.setPeerUid(getPeerUid());
    return joinGameMessage;
  }

  public InetSocketAddress getPeerAddress() {
    return getSocketAddress(PEER_ADDRESS_INDEX);
  }

  public void setPeerAddress(InetSocketAddress peerAddress) {
    setValue(PEER_ADDRESS_INDEX, SocketAddressUtil.toString(peerAddress));
  }

  public String getUsername() {
    return getString(USERNAME_INDEX);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
  }

  public int getPeerUid() {
    return getInt(PEER_UID_INDEX);
  }

  public void setPeerUid(int peerUid) {
    setValue(PEER_UID_INDEX, peerUid);
  }
}
