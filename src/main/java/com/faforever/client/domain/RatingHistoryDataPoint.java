package com.faforever.client.domain;

import java.time.LocalDateTime;

public class RatingHistoryDataPoint {
  private LocalDateTime dateTime;
  private float mean;
  private float deviation;

  public RatingHistoryDataPoint(LocalDateTime dateTime, float mean, float deviation) {
    this.dateTime = dateTime;
    this.mean = mean;
    this.deviation = deviation;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(LocalDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public float getMean() {
    return mean;
  }

  public void setMean(float mean) {
    this.mean = mean;
  }

  public float getDeviation() {
    return deviation;
  }

  public void setDeviation(float deviation) {
    this.deviation = deviation;
  }
}
