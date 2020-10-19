package com.faforever.client.remote.domain;

import org.jetbrains.annotations.NotNull;

public class RatingRange implements Comparable<RatingRange> {

  private final Integer min;
  private final Integer max;

  public RatingRange(Integer min, Integer max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public int compareTo(@NotNull RatingRange o) {
    Integer otherMin = o.getMin();

    if (min == null) {
      return otherMin == null ? 0 : otherMin;
    }

    return Integer.compare(min, o.getMin());
  }

  public Integer getMin() {
    return min;
  }

  public Integer getMax() {
    return max;
  }
}
