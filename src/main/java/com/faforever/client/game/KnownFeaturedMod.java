package com.faforever.client.game;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of "known" featured mods. They might be added and removed to the server arbitrarily, which is why
 * the client should rely as little as possible on this static definition.
 */
public enum KnownFeaturedMod {
  FAF("faf"),
  FAF_BETA("fafbeta"),
  FAF_DEVELOP("fafdevelop"),
  BALANCE_TESTING("balancetesting"),
  LADDER_1V1("ladder1v1"),
  COOP("coop"),
  GALACTIC_WAR("fafdevelop"), // TODO: remove this once featured mod is in place
  MATCHMAKER("matchmaker");

  public static final KnownFeaturedMod DEFAULT = FAF;

  private static final Map<String, KnownFeaturedMod> fromString;

  static {
    fromString = new HashMap<>();
    for (KnownFeaturedMod knownFeaturedMod : values()) {
      fromString.put(knownFeaturedMod.technicalName, knownFeaturedMod);
    }
  }

  private final String technicalName;

  KnownFeaturedMod(String technicalName) {
    this.technicalName = technicalName;
  }

  public static KnownFeaturedMod fromString(String string) {
    return fromString.get(string);
  }

  public String getTechnicalName() {
    return technicalName;
  }
}
