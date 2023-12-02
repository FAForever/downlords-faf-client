package com.faforever.client.game;

import org.jetbrains.annotations.NotNull;

public record RatingRange(Integer min, Integer max) implements Comparable<RatingRange> {

  public RatingRange {
    if (min != null && max != null && min > max) {
      throw new IllegalArgumentException("Invalid max %d and min %d".formatted(max, min));
    }
  }

  @Override
  public int compareTo(@NotNull RatingRange o) {
    Integer otherMin = o.min();
    Integer otherMax = o.max();
    if (min != null || otherMin != null) {
      if (min == null) {
        return -1;
      }

      if (otherMin == null) {
        return 1;
      }

      return Integer.compare(min, otherMin);
    } else if (max != null || otherMax != null) {
      if (max == null) {
        return -1;
      }

      if (otherMax == null) {
        return 1;
      }

      return Integer.compare(max, otherMax);
    } else {
      return 0;
    }
  }
}
