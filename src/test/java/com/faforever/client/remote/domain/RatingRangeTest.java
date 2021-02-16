package com.faforever.client.remote.domain;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.assertEquals;

public class RatingRangeTest {

  private RatingRange ratingRange;

  @Test
  @DisplayName("Confirm one as minimum")
  public void testGetMinWithIntegers(){
      ratingRange = new RatingRange(1, 2);
      assertEquals(1, ratingRange.getMin().intValue());
  }

  @Test
  @DisplayName("Confirm null as minimum, treating as zero")
  public void testGetMinWithNulls(){
    ratingRange = new RatingRange(null, 2);
    assertEquals(0, ratingRange.getMin().intValue());
  }
}
