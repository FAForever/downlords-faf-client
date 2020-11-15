package com.faforever.client.remote.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public enum GameType {

  UNKNOWN("unknown"),
  CUSTOM("custom"),
  MATCHMAKER("matchmaker"),
  COOP("coop"),
  TUTORIAL("tutorial");

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

  public static GameType fromString(String string) {
    GameType gameType = fromString.get(string != null ? string.toLowerCase(Locale.US) : null);
    if (gameType == null) {
      log.warn("Unknown game type: {}", string);
      return UNKNOWN;
    }
    return gameType;
  }

  public String getString() {
    return string;
  }
}
