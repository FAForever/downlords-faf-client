package com.faforever.client.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public interface TimeService {

  /**
   * A string as "10 minutes ago"
   */
  String timeAgo(Instant instant);

  /**
   * Returns {@link #timeAgo(Instant)} if the specified instant is less than one day ago, otherwise a date string.
   */
  String lessThanOneDayAgo(Instant instant);

  String asDate(TemporalAccessor temporalAccessor);

  String asShortTime(Instant instant);

  String shortDuration(Duration duration);
}
