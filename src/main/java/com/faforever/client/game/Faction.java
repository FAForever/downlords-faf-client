package com.faforever.client.game;

import java.util.HashMap;

public enum Faction {
  UEF("uef"),
  AEON("aeon"),
  CYBRAN("cybran"),
  SERAPHIM("seraphim");

  private static final HashMap<String, Faction> fromId;

  static {
    fromId = new HashMap<>();
    for (Faction faction : values()) {
      fromId.put(faction.string, faction);
    }
  }

  private String string;

  Faction(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static Faction fromString(String factionId) {
    return fromId.get(factionId);
  }
}
