package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class AcceptPartyInviteMessage extends ClientMessage {

  private final Integer senderId;

  public AcceptPartyInviteMessage(Integer senderId) {
    super(ClientMessageType.ACCEPT_PARTY_INVITE);
    this.senderId = senderId;
  }

}
