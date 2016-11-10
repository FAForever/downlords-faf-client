package com.faforever.client.fa.relay;

import com.faforever.client.remote.domain.MessageTarget;

import static com.faforever.client.fa.relay.GpgServerMessageType.CONNECT_TO_PEER;

public class ConnectToPeerMessage extends GpgServerMessage {

  private static final int USERNAME_INDEX = 0;
  private static final int PEER_UID_INDEX = 1;

  public ConnectToPeerMessage() {
    super(CONNECT_TO_PEER, 2);
    setTarget(MessageTarget.GAME);
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

}
