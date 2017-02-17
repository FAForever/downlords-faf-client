package com.faforever.client.domain;

import lombok.Data;

import java.time.Instant;

@Data
public class RatingHistoryDataPoint {
  private final Instant instant;
  private final float mean;
  private final float deviation;
}
