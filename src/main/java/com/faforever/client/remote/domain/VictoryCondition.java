package com.faforever.client.remote.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public enum VictoryCondition {
  // Order is crucial
  DEMORALIZATION(0),
  DOMINATION(1),
  ERADICATION(2),
  SANDBOX(3),
  UNKNOWN("unknown");

  private static final Map<Object, VictoryCondition> fromNumber;

  static {
    fromNumber = new HashMap<>();
    for (VictoryCondition victoryCondition : values()) {
      fromNumber.put(victoryCondition.value, victoryCondition);
    }
  }

  private final Object value;

  VictoryCondition(Object value) {
    this.value = value;
  }

  public static VictoryCondition fromNumber(Object number) {
    VictoryCondition victoryCondition = fromNumber.get(number);
    if (victoryCondition == null) {
      log.warn("Unknown victory condition: {}", number);
      return UNKNOWN;
    }
    return victoryCondition;
  }

  public Object getValue() {
    return value;
  }
}
