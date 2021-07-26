package com.faforever.client.player;

public class PlayerOfflineEvent {
  private final String username;

  public PlayerOfflineEvent(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

}
