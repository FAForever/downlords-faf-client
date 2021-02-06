package com.faforever.client.game;

import lombok.Value;

/**
 * Fired whenever the information of a game has changed.
 */
@Value
public class GameUpdatedEvent {
  Game game;
}
