package com.faforever.client.api;

public enum RatingType {
  GLOBAL("global"),
  LADDER_1V1("1v1"),;

  private String string;

  RatingType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
