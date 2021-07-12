package com.faforever.client.remote.domain.inbound.faf;


import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class SessionMessage extends FafInboundMessage {
  public static final String COMMAND = "session";

  long session;
}
