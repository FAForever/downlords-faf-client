package com.faforever.client.remote.domain;

import java.util.List;

public class PlayersMessage extends FafServerMessage {

  private List<PlayerInfo> players;

  public PlayersMessage() {
    super(FafServerMessageType.PLAYER_INFO);
  }

  public List<PlayerInfo> getPlayers() {
    return players;
  }

  public void setPlayers(List<PlayerInfo> playerInfos) {
    this.players = playerInfos;
  }
}
