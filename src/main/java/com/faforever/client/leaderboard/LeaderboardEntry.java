package com.faforever.client.leaderboard;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LeaderboardEntry {

  private final StringProperty username;
  private final DoubleProperty rating;
  private final IntegerProperty gamesPlayed;
  private final FloatProperty winLossRatio;
  private final ObjectProperty<Leaderboard> leaderboard;

  public LeaderboardEntry() {
    username = new SimpleStringProperty();
    rating = new SimpleDoubleProperty();
    gamesPlayed = new SimpleIntegerProperty();
    winLossRatio = new SimpleFloatProperty();
    leaderboard = new SimpleObjectProperty<>();
  }

  public static LeaderboardEntry fromDto(com.faforever.client.api.dto.LeaderboardEntry entry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setLeaderboard(Leaderboard.fromDto(entry.getLeaderboard()));
    leaderboardEntry.setUsername(entry.getPlayer().getLogin());
    leaderboardEntry.setRating(entry.getRating());
    leaderboardEntry.setWinLossRatio(entry.getWonGames() / (float) entry.getTotalGames());
    leaderboardEntry.setGamesPlayed(entry.getTotalGames());
    return leaderboardEntry;
  }

  public String getUsername() {
    return username.get();
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public Leaderboard getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(Leaderboard leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public ObjectProperty<Leaderboard> leaderboardProperty() {
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

  @Override
  public int hashCode() {
    return username.get() != null ? username.get().hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LeaderboardEntry that = (LeaderboardEntry) o;

    return !(username.get() != null ? !username.get().equalsIgnoreCase(that.username.get()) : that.username.get() != null);

  }

  @Override
  public String toString() {
    return "LeaderboardEntry{" +
        "username=" + username.get() +
        ",leaderboard=" + leaderboard.get().getTechnicalName() +
        '}';
  }
}
