package com.faforever.client.domain;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RatingHistoryDataPoint {
  private final OffsetDateTime instant;
  private final float mean;
  private final float deviation;
}
