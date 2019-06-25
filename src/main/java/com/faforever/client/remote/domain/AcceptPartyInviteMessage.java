package com.faforever.client.remote.domain;

public class AcceptPartyInviteMessage extends ClientMessage {

  private Integer sender_id;

  public AcceptPartyInviteMessage(Integer sender_id) {
    super(ClientMessageType.ACCEPT_PARTY_INVITE);
    this.sender_id = sender_id;
  }

  public Integer getSender_id() {
    return sender_id;
  }

  public void setSender_id(Integer sender_id) {
    this.sender_id = sender_id;
  }
}
