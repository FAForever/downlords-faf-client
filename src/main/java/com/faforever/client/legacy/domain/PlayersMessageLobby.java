package com.faforever.client.legacy.domain;

import java.util.List;

public class PlayersMessageLobby extends FafServerMessage {

  private List<Player> players;

  public PlayersMessageLobby() {
    super(FafServerMessageType.PLAYER_INFO);
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }
}
