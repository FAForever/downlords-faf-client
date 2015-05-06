package com.faforever.client.legacy.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum GameType {
  DEMORALIZATION(0),
  DOMINATION(1),
  ERADICATION(2),
  SANDBOX(3),
  UNKNOWN(null);

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Integer number;

  private static final Map<Integer, GameType> fromNumber;

  public static GameType fromNumber(Integer number) {
    GameType gameType = fromNumber.get(number);
    if (gameType == null) {
      logger.warn("Unknown game type: {}", number);
      return UNKNOWN;
    }
    return gameType;
  }

  static {
    fromNumber = new HashMap<>();
    for (GameType gameType : values()) {
      fromNumber.put(gameType.number, gameType);
    }
  }

  GameType(Integer number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }
}
