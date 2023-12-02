package com.faforever.client.domain;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class LeaderboardEntryBean extends AbstractEntityBean {

  @ToString.Include
  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final DoubleProperty rating = new SimpleDoubleProperty();
  private final IntegerProperty gamesPlayed = new SimpleIntegerProperty();
  private final FloatProperty winLossRatio = new SimpleFloatProperty();
  @ToString.Include
  private final ObjectProperty<LeaderboardBean> leaderboard = new SimpleObjectProperty<>();

  public PlayerBean getPlayer() {
    return player.get();
  }

  public void setPlayer(PlayerBean player) {
    this.player.set(player);
  }

  public ObjectProperty<PlayerBean> playerProperty() {
    return player;
  }

  public LeaderboardBean getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(LeaderboardBean leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public ObjectProperty<LeaderboardBean> leaderboardProperty() {
    return leaderboard;
  }

  public double getRating() {
    return rating.get();
  }

  public void setRating(double rating) {
    this.rating.set(rating);
  }

  public DoubleProperty ratingProperty() {
    return rating;
  }

  public int getGamesPlayed() {
    return gamesPlayed.get();
  }

  public void setGamesPlayed(int gamesPlayed) {
    this.gamesPlayed.set(gamesPlayed);
  }

  public IntegerProperty gamesPlayedProperty() {
    return gamesPlayed;
  }

  public float getWinLossRatio() {
    return winLossRatio.get();
  }

  public void setWinLossRatio(float winLossRatio) {
    this.winLossRatio.set(winLossRatio);
  }

  public FloatProperty winLossRatioProperty() {
    return winLossRatio;
  }
}
