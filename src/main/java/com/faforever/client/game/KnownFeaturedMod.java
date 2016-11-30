package com.faforever.client.game;

import java.util.HashMap;
import java.util.Map;

public enum KnownFeaturedMod {
  FAF("faf"),
  FAF_BETA("fafbeta"),
  FAF_DEVELOP("fafdevelop"),
  BALANCE_TESTING("balancetesting"),
  LADDER_1V1("ladder1v1"),
  COOP("coop"),
  GALACTIC_WAR("gw"),
  MATCHMAKER("matchmaker");

  public static final KnownFeaturedMod DEFAULT = FAF;

  private static final Map<String, KnownFeaturedMod> fromString;

  static {
    fromString = new HashMap<>();
    for (KnownFeaturedMod knownFeaturedMod : values()) {
      fromString.put(knownFeaturedMod.string, knownFeaturedMod);
    }
  }

  private final String string;

  KnownFeaturedMod(String string) {
    this.string = string;
  }

  public static KnownFeaturedMod fromString(String string) {
    return fromString.get(string);
  }

  public String getString() {
    return string;
  }
}
