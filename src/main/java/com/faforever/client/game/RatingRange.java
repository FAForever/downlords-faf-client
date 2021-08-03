package com.faforever.client.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Value
public class RatingRange implements Comparable<RatingRange> {

  Integer min;
  Integer max;

  public RatingRange(Integer min, Integer max) {
    this.min = min;
    this.max = max;
  }

  @JsonCreator
  public RatingRange(List<Integer> range) {
    if (range.size() > 0) {
      this.min = range.get(0);
    } else {
      this.min = null;
    }

    if (range.size() > 1) {
      this.max = range.get(1);
    } else {
      this.max = null;
    }
  }

  @JsonValue
  public List<Integer> getRange() {
    return List.of(min, max);
  }

  @Override
  public int compareTo(@NotNull RatingRange o) {
    Integer otherMin = o.getMin();
    Integer otherMax = o.getMax();
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
