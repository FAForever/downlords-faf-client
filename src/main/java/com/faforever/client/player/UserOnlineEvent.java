package com.faforever.client.player;

public class UserOnlineEvent {
  private final String username;

  public UserOnlineEvent(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

}
