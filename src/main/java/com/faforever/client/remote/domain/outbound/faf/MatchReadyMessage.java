package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * When matchmaker finds a match, all players needs to ready up for game to get
 * launched.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class MatchReadyMessage extends FafOutboundMessage {
  public static final String COMMAND = "match_ready";
  
  boolean ready;
}
