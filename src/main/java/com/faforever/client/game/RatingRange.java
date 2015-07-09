package com.faforever.client.game;

public class RatingRange implements Comparable<RatingRange> {

  public final Integer min;
  public final Integer max;

  public RatingRange(Integer min, Integer max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public int compareTo(RatingRange o) {
    return Integer.compare(min, o.min);
  }
}
