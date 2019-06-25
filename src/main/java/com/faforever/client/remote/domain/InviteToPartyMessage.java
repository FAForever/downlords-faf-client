package com.faforever.client.remote.domain;

public class InviteToPartyMessage extends ClientMessage {

  private Integer recipient_id;

  public InviteToPartyMessage(Integer recipient_id) {
    super(ClientMessageType.INVITE_TO_PARTY);
    this.recipient_id = recipient_id;
  }

  public Integer getRecipient_id() {
    return recipient_id;
  }

  public void setRecipient_id(Integer recipient_id) {
    this.recipient_id = recipient_id;
  }
}
