package com.faforever.client.domain;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Represents a leaderboard rating
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LeaderboardRatingBean {

  FloatProperty deviation = new SimpleFloatProperty();
  FloatProperty mean = new SimpleFloatProperty();
  IntegerProperty numberOfGames = new SimpleIntegerProperty();

  public int getNumberOfGames() {
    return numberOfGames.get();
  }

  public void setNumberOfGames(int numberOfGames) {
    this.numberOfGames.set(numberOfGames);
  }

  public IntegerProperty numberOfGamesProperty() {
    return numberOfGames;
  }

  public float getDeviation() {
    return deviation.get();
  }

  public void setDeviation(float deviation) {
    this.deviation.set(deviation);
  }

  public FloatProperty deviationProperty() {
    return deviation;
  }

  public float getMean() {
    return mean.get();
  }

  public void setMean(float mean) {
    this.mean.set(mean);
  }

  public FloatProperty meanProperty() {
    return mean;
  }
}
