package com.faforever.client.relay;

import com.faforever.client.remote.domain.MessageTarget;

import static com.faforever.client.relay.GpgServerMessageType.CONNECT_TO_PEER;
import static com.github.nocatch.NoCatch.noCatch;

public class ConnectToPeerMessage extends GpgServerMessage implements Cloneable {

  private static final int USERNAME_INDEX = 0;
  private static final int PEER_UID_INDEX = 1;

  public ConnectToPeerMessage() {
    super(CONNECT_TO_PEER, 2);
    setTarget(MessageTarget.GAME);
  }

  @Override
  public ConnectToPeerMessage clone() {
    noCatch(() -> super.clone());
    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();
    connectToPeerMessage.setUsername(getUsername());
    connectToPeerMessage.setPeerUid(getPeerUid());
    return connectToPeerMessage;
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

  public void setPeerUid(int uid) {
    setValue(PEER_UID_INDEX, uid);
  }

}
