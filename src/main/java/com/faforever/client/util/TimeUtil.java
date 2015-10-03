package com.faforever.client.util;

import java.time.Instant;

public final class TimeUtil {

  private TimeUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static Instant fromPythonTime(double time) {
    return Instant.ofEpochMilli((long) (time * 1000));
  }
}
