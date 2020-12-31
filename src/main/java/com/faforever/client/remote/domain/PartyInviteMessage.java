package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartyInviteMessage extends FafServerMessage {

  private Integer sender;

  public PartyInviteMessage() {
    super(FafServerMessageType.PARTY_INVITE);
  }

}
