package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;


public final class PingMessage extends FafOutboundMessage {
  public static final String COMMAND = "ping";
}
