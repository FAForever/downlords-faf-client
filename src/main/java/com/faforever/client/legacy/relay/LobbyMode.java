package com.faforever.client.legacy.relay;

public enum LobbyMode {

  /**
   * The lobby is skipped; the game starts straight away,
   */
  NO_LOBBY(0),

  /**
   * Default lobby where players can select their faction, teams and so on.
   */
  DEFAULT_LOBBY(1),;

  private int mode;

  LobbyMode(int mode) {
    this.mode = mode;
  }

  public int getMode() {
    return mode;
  }
}
