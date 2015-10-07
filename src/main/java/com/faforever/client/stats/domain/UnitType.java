package com.faforever.client.stats.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum UnitType {
  ACU("Armored Command Unit"),
  ENGINEER("Engineer"),
  MEDIUM_TANK("Medium Tank"),
  UNKNOWN(null);

  private static final Pattern TYPE_PATTERN = Pattern.compile("<LOC.*?>(.*)");

  private static final Map<String, UnitType> fromString;

  static {
    fromString = new HashMap<>();
    for (UnitType unitType : values()) {
      fromString.put(unitType.string, unitType);
    }
  }

  private String string;

  UnitType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static UnitType fromString(String string) {
    Matcher matcher = TYPE_PATTERN.matcher(string);
    if (!matcher.find()) {
      return UNKNOWN;
    }

    UnitType unitType = fromString.get(matcher.group(1));
    if (unitType == null) {
      return UNKNOWN;
    }
    return unitType;
  }
}
