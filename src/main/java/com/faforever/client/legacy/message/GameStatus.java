package com.faforever.client.legacy.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum GameStatus {

  UNKNOWN(null),
  PLAYING("playing"),
  OPEN("open"),
  CLOSED("closed");

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String string;

  private static final Map<String, GameStatus> fromString;

  public static GameStatus fromString(String string) {
    GameStatus gameStatus = fromString.get(string);
    if (gameStatus == null) {
      logger.warn("Unknown game state: {}", string);
      return UNKNOWN;
    }
    return gameStatus;
  }

  static {
    fromString = new HashMap<>();
    for (GameStatus gamestate : values()) {
      fromString.put(gamestate.string, gamestate);
    }
  }

  GameStatus(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
