package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class InviteToPartyMessage extends FafOutboundMessage {
  public static final String COMMAND = "invite_to_party";

  Integer recipientId;
}
