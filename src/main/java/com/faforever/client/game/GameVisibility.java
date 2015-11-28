package com.faforever.client.game;

import java.util.HashMap;

public enum GameVisibility {
  PUBLIC("public"),
  PRIVATE("friends");

  private static final HashMap<String, GameVisibility> fromString;

  static {
    fromString = new HashMap<>();
    for (GameVisibility gameVisibility : values()) {
      fromString.put(gameVisibility.string, gameVisibility);
    }
  }

  private String string;

  GameVisibility(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static GameVisibility fromString(String string) {
    return fromString.get(string);
  }
}
