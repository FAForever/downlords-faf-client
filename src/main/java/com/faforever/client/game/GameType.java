package com.faforever.client.game;

import java.util.HashMap;
import java.util.Map;

public enum GameType {
  FAF("faf"),
  FAF_BETA("fafbeta"),
  BALANCE_TESTING("balancetesting"),
  LADDER_1V1("ladder1v1"),
  COOP("coop");

  public static final GameType DEFAULT = FAF;

  private static final Map<String, GameType> fromString;

  static {
    fromString = new HashMap<>();
    for (GameType gameType : values()) {
      fromString.put(gameType.string, gameType);
    }
  }

  private final String string;

  GameType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static GameType fromString(String string) {
    return fromString.get(string);
  }
}
