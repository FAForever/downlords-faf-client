package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class InviteToPartyMessage extends ClientMessage {

  private final Integer recipientId;

  public InviteToPartyMessage(Integer recipientId) {
    super(ClientMessageType.INVITE_TO_PARTY);
    this.recipientId = recipientId;
  }

}
