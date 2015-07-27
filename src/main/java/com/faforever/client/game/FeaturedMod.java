package com.faforever.client.game;

public enum FeaturedMod {
  FAF("faf"),
  BALANCE_TESTING("balancetesting"),
  LADDER_1V1("ladder1v1");

  public static FeaturedMod DEFAULT_MOD = FAF;

  private final String string;

  FeaturedMod(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
