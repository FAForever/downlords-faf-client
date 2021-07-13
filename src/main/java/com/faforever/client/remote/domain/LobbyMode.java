package com.faforever.client.remote.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

/**
 * See values for description.
 */

@JsonFormat(shape = Shape.NUMBER_INT)
public enum LobbyMode {

  /**
   * Default lobby where players can select their faction, teams and so on.
   */
  DEFAULT_LOBBY,

  /**
   * The lobby is skipped; the preferences starts straight away,
   */
  AUTO_LOBBY
}
