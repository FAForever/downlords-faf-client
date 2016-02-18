package com.faforever.client.remote.domain;

import java.util.List;

public class PlayersMessage extends FafServerMessage {

  private List<Player> players;

  public PlayersMessage() {
    super(FafServerMessageType.PLAYER_INFO);
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }
}
