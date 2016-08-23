package com.faforever.client.user.event;

public class LoginSuccessEvent {
  private final String username;

  public LoginSuccessEvent(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
