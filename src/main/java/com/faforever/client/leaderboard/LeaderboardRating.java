package com.faforever.client.leaderboard;

import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a leaderboard rating
 */
public class LeaderboardRating {

  private final FloatProperty deviation;
  private final FloatProperty mean;
  private final IntegerProperty numberOfGames;

  @VisibleForTesting
  LeaderboardRating() {
    deviation = new SimpleFloatProperty();
    mean = new SimpleFloatProperty();
    numberOfGames = new SimpleIntegerProperty();
  }

  public static LeaderboardRating fromDto(com.faforever.client.remote.domain.LeaderboardRating dto) {
    LeaderboardRating leaderboardRating = new LeaderboardRating();
    leaderboardRating.setNumberOfGames(Optional.ofNullable(dto.getNumberOfGames()).orElse(0));
    leaderboardRating.setMean(Optional.ofNullable(dto.getRating()).map(rating -> rating[0]).orElse(0f));
    leaderboardRating.setDeviation(Optional.ofNullable(dto.getRating()).map(rating -> rating[1]).orElse(0f));
    return leaderboardRating;
  }

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

  @Override
  public int hashCode() {
    return Objects.hash(deviation.get(), mean.get(), numberOfGames.get());
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && (obj.getClass() == LeaderboardRating.class)
        && getMean() == ((LeaderboardRating) obj).getMean()
        && getDeviation() == ((LeaderboardRating) obj).getDeviation()
        && getNumberOfGames() == ((LeaderboardRating) obj).getNumberOfGames();
  }

}
