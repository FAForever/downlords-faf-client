package com.faforever.client.util;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

public interface TimeService {

  /**
   * A string as "10 minutes ago"
   */
  String timeAgo(Temporal temporal);

  /**
   * Returns {@link #timeAgo(Temporal)} if the specified instant is less than one day ago, otherwise a date string.
   */
  String lessThanOneDayAgo(Temporal temporal);

  String asDate(TemporalAccessor temporalAccessor);

  String asShortTime(Temporal temporal);

  String asIsoTime(Temporal temporal);

  /**
   * Returns the localized minutes and seconds (e.g. '20min 31s'), or hours and minutes (e.g. '1h 5min') of the
   * specified duration.
   */
  String shortDuration(Duration duration);

  /**
   * Returns e.g. "3:21:12" (h:mm:ss).
   */
  String asHms(Duration duration);
}
