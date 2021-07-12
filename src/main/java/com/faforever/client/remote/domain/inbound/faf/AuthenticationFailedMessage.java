package com.faforever.client.remote.domain.inbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class AuthenticationFailedMessage extends FafInboundMessage {
  public static final String COMMAND = "authentication_failed";

  String text;
}
