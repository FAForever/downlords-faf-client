package com.faforever.client.game;

import java.util.HashMap;

public enum Faction {
  UEF(1),
  AEON(2),
  CYBRAN(3),
  SERAPHIM(4);

  private static final HashMap<Integer, Faction> fromId;

  static {
    fromId = new HashMap<>();
    for (Faction faction : values()) {
      fromId.put(faction.id, faction);
    }
  }

  private int id;

  Faction(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static Faction fromId(int factionId) {
    return fromId.get(factionId);
  }
}
