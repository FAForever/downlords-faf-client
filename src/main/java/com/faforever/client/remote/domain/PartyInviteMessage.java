package com.faforever.client.remote.domain;

public class PartyInviteMessage extends FafServerMessage {

  private Integer sender;

  public PartyInviteMessage() {
    super(FafServerMessageType.PARTY_INVITE);
  }

  public Integer getSender() {
    return sender;
  }

  public void setSender(Integer sender) {
    this.sender = sender;
  }
}
