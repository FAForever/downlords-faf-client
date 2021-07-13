package com.faforever.client.remote.domain.inbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class MatchCancelledMessage extends FafInboundMessage {
  public static final String COMMAND = "match_cancelled";
}
