package com.faforever.client.remote.domain.inbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class PartyInviteMessage extends FafInboundMessage {
  public static final String COMMAND = "party_invite";

  Integer sender;
}
