package com.faforever.client.game;

import java.util.HashMap;

public enum Faction {
  UEF("uef"),
  AEON("aeon"),
  CYBRAN("cybran"),
  SERAPHIM("seraphim");

  private static final HashMap<String, Faction> fromString;

  static {
    fromString = new HashMap<>();
    for (Faction faction : values()) {
      fromString.put(faction.string, faction);
    }
  }

  private String string;

  Faction(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static Faction fromString(String string) {
    return fromString.get(string);
  }
}
