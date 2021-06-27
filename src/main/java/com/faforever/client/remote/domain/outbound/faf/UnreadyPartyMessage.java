package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;


public class UnreadyPartyMessage extends FafOutboundMessage {
  public static final String COMMAND = "unready_party";
}
