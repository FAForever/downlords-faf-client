package com.faforever.client.game;

import java.util.HashMap;
import java.util.Map;

public enum FeaturedMod {
  FAF("faf"),
  BALANCE_TESTING("balancetesting"),
  LADDER_1V1("ladder1v1");

  public static final FeaturedMod DEFAULT_MOD = FAF;

  private static final Map<String, FeaturedMod> fromString;

  static {
    fromString = new HashMap<>();
    for (FeaturedMod featuredMod : values()) {
      fromString.put(featuredMod.string, featuredMod);
    }
  }

  private final String string;

  FeaturedMod(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static FeaturedMod fromString(String string) {
    return fromString.get(string);
  }
}
