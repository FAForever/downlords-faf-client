package com.faforever.client.fa;

public enum RatingMode {
  GLOBAL,
  LADDER_1V1,
  NONE;

  public String getI18NKey() {
    return switch (this) {
      case GLOBAL -> "userInfo.ratingHistory.global";
      case LADDER_1V1 -> "userInfo.ratingHistory.1v1";
      default -> "";
    };
  }
}
