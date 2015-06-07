package com.faforever.client.legacy.domain;

public enum StatisticsType {
  LEAGUE_TABLE("legue_table");

  private String string;

  StatisticsType(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }
}
