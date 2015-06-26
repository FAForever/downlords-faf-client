package com.faforever.client.legacy.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public enum VictoryCondition {
  DEMORALIZATION(0),
  DOMINATION(1),
  ERADICATION(2),
  SANDBOX(3),
  UNKNOWN(null);

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Integer number;

  private static final Map<Integer, VictoryCondition> fromNumber;

  public static VictoryCondition fromNumber(Integer number) {
    VictoryCondition victoryCondition = fromNumber.get(number);
    if (victoryCondition == null) {
      logger.warn("Unknown victory condition: {}", number);
      return UNKNOWN;
    }
    return victoryCondition;
  }

  static {
    fromNumber = new HashMap<>();
    for (VictoryCondition victoryCondition : values()) {
      fromNumber.put(victoryCondition.number, victoryCondition);
    }
  }

  VictoryCondition(Integer number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }
}
