package com.faforever.client.remote.domain;

public class KickPlayerFromPartyMessage extends ClientMessage {

  private Integer kicked_player_id;

  public KickPlayerFromPartyMessage(Integer kicked_player_id) {
    super(ClientMessageType.KICK_PLAYER_FROM_PARTY);
    this.kicked_player_id = kicked_player_id;
  }

  public Integer getKicked_player_id() {
    return kicked_player_id;
  }

  public void setKicked_player_id(Integer kicked_player_id) {
    this.kicked_player_id = kicked_player_id;
  }
}
