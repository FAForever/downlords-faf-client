package com.faforever.client.legacy.domain;

import java.util.HashMap;
import java.util.Map;

public enum StatisticsType {
  LEAGUE_TABLE("league_table"),
  STATS("stats"),
  GLOBAL_90_DAYS("global_90_days"),
  GLOBAL_365_DAYS("global_365_days"),
  UNKNOWN("unknown");

  private static final Map<String, StatisticsType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (StatisticsType statisticsType : values()) {
      fromString.put(statisticsType.string, statisticsType);
    }
  }

  private String string;

  StatisticsType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static StatisticsType fromString(String string) {
    return fromString.get(string);
  }
}
