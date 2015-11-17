package com.faforever.client.legacy.domain;

import java.util.List;

public class PlayerInfo extends ServerMessage {

  private List<Player> players;

  public PlayerInfo() {
    super(ServerMessageType.PLAYER_INFO);
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }
}
