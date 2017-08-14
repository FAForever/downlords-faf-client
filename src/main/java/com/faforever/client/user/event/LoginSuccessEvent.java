package com.faforever.client.user.event;

public class LoginSuccessEvent {
  private final String username;
  private final String password;
  private final int userId;

  public LoginSuccessEvent(String username, String password, int userId) {
    this.username = username;
    this.password = password;
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public int getUserId() {
    return userId;
  }

  public String getPassword() {
    return password;
  }
}
