package com.faforever.client.player;

import lombok.Getter;

import java.time.LocalDateTime;

public enum TimePeriod {
  ALL_TIME("userInfo.ratingHistory.allTime", LocalDateTime.MIN),
  LAST_YEAR("userInfo.ratingHistory.lastYear", LocalDateTime.now().minusYears(1)),
  LAST_MONTH("userInfo.ratingHistory.lastMonth", LocalDateTime.now().minusMonths(1));

  @Getter
  private final String i18nKey;
  @Getter
  private final LocalDateTime date;

  TimePeriod(String i18nKey, LocalDateTime date) {
    this.i18nKey = i18nKey;
    this.date = date;
  }
}
