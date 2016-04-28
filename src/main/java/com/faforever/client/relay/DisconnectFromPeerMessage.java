package com.faforever.client.relay;

import com.faforever.client.remote.domain.MessageTarget;

public class DisconnectFromPeerMessage extends GpgServerMessage {

  private static final int UID_INDEX = 0;

  public DisconnectFromPeerMessage() {
    super(GpgServerMessageType.DISCONNECT_FROM_PEER, 1);
    setTarget(MessageTarget.GAME);
  }

  public int getUid() {
    return getInt(UID_INDEX);
  }

  public void setUid(int uid) {
    setValue(UID_INDEX, uid);
  }
}
