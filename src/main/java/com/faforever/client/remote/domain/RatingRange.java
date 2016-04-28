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
    return Integer.compare(getMin(), o.getMin());
  }

  public Integer getMin() {
    return min;
  }

  public Integer getMax() {
    return max;
  }
}
