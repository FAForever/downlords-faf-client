package com.faforever.client.remote.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum GameStatus {

  UNKNOWN("unknown"),
  PLAYING("playing"),
  OPEN("open"),
  CLOSED("closed");

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Map<String, GameStatus> fromString;

  static {
    fromString = new HashMap<>();
    for (GameStatus gameStatus : values()) {
      fromString.put(gameStatus.string, gameStatus);
    }
  }

  private final String string;

  GameStatus(String string) {
    this.string = string;
  }

  public static GameStatus fromString(String string) {
    GameStatus gameStatus = fromString.get(string);
    if (gameStatus == null) {
      logger.warn("Unknown game state: {}", string);
      return UNKNOWN;
    }
    return gameStatus;
  }

  public String getString() {
    return string;
  }
}
