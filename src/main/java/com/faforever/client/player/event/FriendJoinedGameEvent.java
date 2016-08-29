package com.faforever.client.player.event;

import com.faforever.client.player.Player;

public class FriendJoinedGameEvent {
  private Player player;

  public FriendJoinedGameEvent(Player player) {
    this.player = player;
  }

  public Player getPlayer() {
    return player;
  }
}
