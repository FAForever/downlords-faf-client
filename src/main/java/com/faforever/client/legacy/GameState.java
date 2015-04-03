package com.faforever.client.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum GameState {

  UNKNOWN(null),
  PLAYING("playing"),
  OPEN("open"),
  CLOSED("closed");

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String string;

  private static final Map<String, GameState> fromString;

  public static GameState fromString(String string) {
    GameState gameState = fromString.get(string);
    if (gameState == null) {
      logger.warn("Unknown game state: {}", string);
      return UNKNOWN;
    }
    return gameState;
  }

  static {
    fromString = new HashMap<>();
    for (GameState gamestate : values()) {
      fromString.put(gamestate.string, gamestate);
    }
  }

  GameState(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
