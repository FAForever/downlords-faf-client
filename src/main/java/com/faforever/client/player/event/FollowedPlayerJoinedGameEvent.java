package com.faforever.client.player.event;

import com.faforever.client.game.Game;
import com.faforever.client.player.Player;
import lombok.Getter;

@Getter
public class FollowedPlayerJoinedGameEvent {
  private final Player player;
  private final Game game;

  public FollowedPlayerJoinedGameEvent(Player player, Game game) {
    this.player = player;
    this.game = game;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof FollowedPlayerJoinedGameEvent)
        && ((FollowedPlayerJoinedGameEvent) obj).player.equals(player)
        && ((FollowedPlayerJoinedGameEvent) obj).game.equals(game);
  }
}
