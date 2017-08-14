package com.faforever.client.player;

public class UserOfflineEvent {
  private final String username;

  public UserOfflineEvent(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

}
