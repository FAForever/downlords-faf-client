package com.faforever.client.fa.relay;

/**
 * See values for description.
 */
public enum LobbyMode {

  /**
   * Default lobby where players can select their faction, teams and so on.
   */
  DEFAULT_LOBBY(0),

  /**
   * The lobby is skipped; the preferences starts straight away,
   */
  AUTO_LOBBY(1);

  private int mode;

  LobbyMode(int mode) {
    this.mode = mode;
  }

  public int getMode() {
    return mode;
  }
}
