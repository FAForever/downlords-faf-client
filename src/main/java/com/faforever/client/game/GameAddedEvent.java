package com.faforever.client.game;

import lombok.Value;

/**
 * Fired whenever the information of a game has been added.
 */
@Value
public class GameAddedEvent {
  private Game game;
}
