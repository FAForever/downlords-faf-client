package com.faforever.client.stats.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum UnitType {
  ACU("Armored Command Unit"),
  ENGINEER("Engineer"),
  MEDIUM_TANK("Medium Tank"),
  PARAGON("Experimental Resource Generator"),
  MAVOR("Experimental Artillery"),
  YOLONA_OSS("Experimental Missile Launcher"),
  CZAR("Experimental Aircraft Carrier"),
  SOUL_RIPPER("Experimental Gunship"),
  AHWASSA("Experimental Bomber"),
  SCATHIS("Experimental Mobile Rapid-Fire Artillery"),
  GALACTIC_COLOSSUS("Experimental Assault Bot"),
  MONKEYLORD("Experimental Spiderbot"),
  MEGALITH("Experimental Megabot"),
  FATBOY("Experimental Mobile Factory"),
  YTHOTHA("Experimental Assault Bot"),
  TEMPEST("Experimental Battleship"),
  ATLANTIS("Experimental Aircraft Carrier"),
  NOVAX_CENTER("Experimental Satellite System"),
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
