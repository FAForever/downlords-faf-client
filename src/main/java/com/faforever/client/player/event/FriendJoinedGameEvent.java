package com.faforever.client.player.event;

import com.faforever.client.game.Game;
import com.faforever.client.player.Player;
import lombok.Getter;

@Getter
public class FriendJoinedGameEvent {
  private Player player;
  private Game game;

  public FriendJoinedGameEvent(Player player, Game game) {
    this.player = player;
    this.game = game;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FriendJoinedGameEvent)
        && ((FriendJoinedGameEvent) obj).player.equals(player)
        && ((FriendJoinedGameEvent) obj).game.equals(game);
  }
}
