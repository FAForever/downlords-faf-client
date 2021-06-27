package com.faforever.client.remote.domain.inbound.gpg;


import com.fasterxml.jackson.annotation.JsonIgnore;

public class GpgJoinGameMessage extends GpgInboundMessage implements Cloneable {
  public static final String COMMAND = "JoinGame";

  private static final int USERNAME_INDEX = 0;
  private static final int PEER_UID_INDEX = 1;

  public GpgJoinGameMessage() {
    super(2);
  }

  @JsonIgnore
  public String getUsername() {
    return getArgAsString(USERNAME_INDEX);
  }

  @JsonIgnore
  public void setUsername(String username) {
    setArgAsValue(USERNAME_INDEX, username);
  }

  @JsonIgnore
  public int getPeerUid() {
    return getArgAsInt(PEER_UID_INDEX);
  }

  @JsonIgnore
  public void setPeerUid(Integer uid) {
    setArgAsValue(PEER_UID_INDEX, uid);
  }
}
