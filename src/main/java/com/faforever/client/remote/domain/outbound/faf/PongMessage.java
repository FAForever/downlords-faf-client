package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;


public class PongMessage extends FafOutboundMessage {
  public static final String COMMAND = "pong";
}
