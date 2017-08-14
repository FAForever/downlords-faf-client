package com.faforever.client.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtil {

  private TimeUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static OffsetDateTime fromPythonTime(double time) {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli((long) (time * 1000)), ZoneId.systemDefault());
  }
}
