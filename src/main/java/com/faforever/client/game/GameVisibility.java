package com.faforever.client.game;

public enum GameVisibility {
  PUBLIC("public"),
  PRIVATE("private");

  private String string;

  GameVisibility(String string) {
    this.string = string;
  }
}
