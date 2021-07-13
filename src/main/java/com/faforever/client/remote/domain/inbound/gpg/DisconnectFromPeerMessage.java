package com.faforever.client.remote.domain.inbound.gpg;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class DisconnectFromPeerMessage extends GpgInboundMessage {
  public static final String COMMAND = "DisconnectFromPeer";

  private static final int UID_INDEX = 0;

  public DisconnectFromPeerMessage() {
    super(1);
  }

  @JsonIgnore
  public int getUid() {
    return getArgAsInt(UID_INDEX);
  }

  @JsonIgnore
  public void setUid(int uid) {
    setArgAsValue(UID_INDEX, uid);
  }
}
