package com.faforever.client.remote.domain.inbound.gpg;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class ConnectToPeerMessage extends GpgInboundMessage {
  public static final String COMMAND = "ConnectToPeer";

  private static final int USERNAME_INDEX = 0;
  private static final int PEER_UID_INDEX = 1;
  private static final int OFFER_INDEX = 2;

  public ConnectToPeerMessage() {
    super(3);
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

  @JsonIgnore
  public boolean isOffer() {
    return getArgAsBoolean(OFFER_INDEX);
  }

  @JsonIgnore
  public void setOffer(Boolean offer) {
    setArgAsValue(OFFER_INDEX, offer);
  }
}
