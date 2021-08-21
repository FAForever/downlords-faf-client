package com.faforever.client.remote.domain.inbound.faf;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class MatchInfoMessage extends FafInboundMessage {
  public static final String COMMAND = "match_info";
  String expiresAt;// TODO if this was a proper ISO date it could probably be auto-parsed to LocalDateTime by jackson
  int playersTotal;
  int playersReady;
  boolean ready;

}
