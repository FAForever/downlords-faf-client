package com.faforever.client.stats;

import java.time.LocalDate;
import java.time.LocalTime;

public class RatingInfo {

  private LocalDate date;
  private float mean;
  private float dev;
  private LocalTime time;

  public RatingInfo() {
  }

  public RatingInfo(LocalDate date, float mean, float dev, LocalTime time) {
    this.date = date;
    this.mean = mean;
    this.dev = dev;
    this.time = time;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public float getMean() {
    return mean;
  }

  public void setMean(float mean) {
    this.mean = mean;
  }

  public float getDev() {
    return dev;
  }

  public void setDev(float dev) {
    this.dev = dev;
  }

  public LocalTime getTime() {
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
  }
}
