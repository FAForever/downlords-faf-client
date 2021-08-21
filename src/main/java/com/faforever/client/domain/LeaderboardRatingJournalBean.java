package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * Represents a leaderboard rating
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class LeaderboardRatingJournalBean extends AbstractEntityBean<LeaderboardRatingJournalBean> {

  ObjectProperty<Double> meanAfter = new SimpleObjectProperty<>();
  ObjectProperty<Double> deviationAfter = new SimpleObjectProperty<>();
  @ToString.Include
  ObjectProperty<Double> meanBefore = new SimpleObjectProperty<>();
  @ToString.Include
  ObjectProperty<Double> deviationBefore = new SimpleObjectProperty<>();
  ObjectProperty<GamePlayerStatsBean> gamePlayerStats = new SimpleObjectProperty<>();
  @ToString.Include
  ObjectProperty<LeaderboardBean> leaderboard = new SimpleObjectProperty<>();

  public Double getMeanAfter() {
    return meanAfter.get();
  }

  public ObjectProperty<Double> meanAfterProperty() {
    return meanAfter;
  }

  public void setMeanAfter(Double meanAfter) {
    this.meanAfter.set(meanAfter);
  }

  public Double getDeviationAfter() {
    return deviationAfter.get();
  }

  public ObjectProperty<Double> deviationAfterProperty() {
    return deviationAfter;
  }

  public void setDeviationAfter(Double deviationAfter) {
    this.deviationAfter.set(deviationAfter);
  }

  public Double getMeanBefore() {
    return meanBefore.get();
  }

  public ObjectProperty<Double> meanBeforeProperty() {
    return meanBefore;
  }

  public void setMeanBefore(Double meanBefore) {
    this.meanBefore.set(meanBefore);
  }

  public Double getDeviationBefore() {
    return deviationBefore.get();
  }

  public ObjectProperty<Double> deviationBeforeProperty() {
    return deviationBefore;
  }

  public void setDeviationBefore(Double deviationBefore) {
    this.deviationBefore.set(deviationBefore);
  }

  public GamePlayerStatsBean getGamePlayerStats() {
    return gamePlayerStats.get();
  }

  public ObjectProperty<GamePlayerStatsBean> gamePlayerStatsProperty() {
    return gamePlayerStats;
  }

  public void setGamePlayerStats(GamePlayerStatsBean gamePlayerStats) {
    this.gamePlayerStats.set(gamePlayerStats);
  }

  public LeaderboardBean getLeaderboard() {
    return leaderboard.get();
  }

  public ObjectProperty<LeaderboardBean> leaderboardProperty() {
    return leaderboard;
  }

  public void setLeaderboard(LeaderboardBean leaderboard) {
    this.leaderboard.set(leaderboard);
  }
}
