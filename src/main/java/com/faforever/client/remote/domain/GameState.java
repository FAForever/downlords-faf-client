package com.faforever.client.remote.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum GameState {

  UNKNOWN("unknown"),
  PLAYING("playing"),
  OPEN("open"),
  CLOSED("closed");

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Map<String, GameState> fromString;

  static {
    fromString = new HashMap<>();
    for (GameState gameState : values()) {
      fromString.put(gameState.string, gameState);
    }
  }

  private final String string;

  GameState(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static GameState fromString(String string) {
    GameState gameState = fromString.get(string);
    if (gameState == null) {
      logger.warn("Unknown game state: {}", string);
      return UNKNOWN;
    }
    return gameState;
  }
}
