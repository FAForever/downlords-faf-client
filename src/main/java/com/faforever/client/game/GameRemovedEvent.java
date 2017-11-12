package com.faforever.client.game;

import lombok.Value;

/**
 * Fired whenever the information of a game has been removed.
 */
@Value
public class GameRemovedEvent {
  private Game game;
}
