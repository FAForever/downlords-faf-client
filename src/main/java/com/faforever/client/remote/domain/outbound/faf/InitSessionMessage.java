package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class InitSessionMessage extends FafOutboundMessage {
  public static final String COMMAND = "ask_session";

  String version;
  String userAgent = "downlords-faf-client";
}
