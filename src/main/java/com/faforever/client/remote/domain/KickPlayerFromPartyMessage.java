package com.faforever.client.remote.domain;

import lombok.Getter;

@Getter
public class KickPlayerFromPartyMessage extends ClientMessage {

  private final Integer kickedPlayerId;

  public KickPlayerFromPartyMessage(Integer kickedPlayerId) {
    super(ClientMessageType.KICK_PLAYER_FROM_PARTY);
    this.kickedPlayerId = kickedPlayerId;
  }

}
