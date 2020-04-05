package com.faforever.client.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum Faction {
  // Order is crucial
  // Same order as the info from the server (1=UEF etc.)
  UEF("uef"),
  AEON("aeon"),
  CYBRAN("cybran"),
  SERAPHIM("seraphim"),
  RANDOM("random"),
  CIVILIAN("civilian");

  private static final Map<String, Faction> fromString;

  static {
    fromString = new HashMap<>();
    for (Faction faction : values()) {
      fromString.put(faction.string, faction);
    }
  }

  private final String string;

  Faction(String string) {
    this.string = string;
  }

  @JsonCreator
  public static Faction fromFaValue(int value) {
    return Faction.values()[value - 1];
  }

  public static Faction fromString(String string) {
    return fromString.get(string);
  }

  /**
   * Returns the faction value used as in "Forged Alliance Forever".
   */
  @JsonValue
  public int toFaValue() {
    return ordinal() + 1;
  }

  /**
   * Returns the string value of the faction, as used in the game and the server.
   */
  public String getString() {
    return string;
  }
}
