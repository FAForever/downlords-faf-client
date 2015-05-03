package com.faforever.client.legacy.message;

public enum GameAccess {
  PUBLIC("public"),
  PASSWORD("password");

  private String string;

  GameAccess(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
