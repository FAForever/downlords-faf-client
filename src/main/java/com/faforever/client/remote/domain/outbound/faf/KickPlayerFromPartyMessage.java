package com.faforever.client.remote.domain.outbound.faf;


import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class KickPlayerFromPartyMessage extends FafOutboundMessage {
  public static final String COMMAND = "kick_player_from_party";

  Integer kickedPlayerId;
}
