package com.faforever.client.player.event;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;

public class FriendJoinedGameEvent {
  private PlayerBean player;
  private GameBean game;

  public FriendJoinedGameEvent(PlayerBean player, GameBean game) {
    this.player = player;
    this.game = game;
  }

  public PlayerBean getPlayer() {
    return player;
  }


  public GameBean getGame() {
    return game;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FriendJoinedGameEvent)
        && ((FriendJoinedGameEvent) obj).player.equals(player)
        && ((FriendJoinedGameEvent) obj).game.equals(game);
  }
}
