package com.faforever.client.preferences;

public class LoginPrefs {

  private String username;
  private String password;

  public LoginPrefs() {
  }

  public LoginPrefs setUsername(String username) {
    this.username = username;
    return this;
  }

  public LoginPrefs setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
