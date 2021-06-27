package com.faforever.client.remote.domain.inbound.faf;

import lombok.Builder;


public class PartyKickedMessage extends FafInboundMessage {
  public static final String COMMAND = "kicked_from_party";
}
