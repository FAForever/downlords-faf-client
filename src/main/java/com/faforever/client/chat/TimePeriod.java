package com.faforever.client.chat;

public enum TimePeriod {
  ALL_TIME,
  LAST_YEAR,
  LAST_MONTH;

  public String getI18NKey() {
    return switch (this) {
      case ALL_TIME -> "userInfo.ratingHistory.allTime";
      case LAST_YEAR -> "userInfo.ratingHistory.lastYear";
      case LAST_MONTH -> "userInfo.ratingHistory.lastMonth";
      default -> "";
    };
  }
}
