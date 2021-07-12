package com.faforever.client.remote.domain.outbound.faf;


import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class AcceptPartyInviteMessage extends FafOutboundMessage {
  public static final String COMMAND = "accept_party_invite";

  Integer senderId;
}
