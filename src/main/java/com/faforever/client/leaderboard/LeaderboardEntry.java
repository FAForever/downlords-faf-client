package com.faforever.client.leaderboard;

import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class LeaderboardEntry {

  private StringProperty username;
  private StringProperty id;
  private IntegerProperty rank;
  private DoubleProperty rating;
  private IntegerProperty gamesPlayed;
  private FloatProperty winLossRatio;

  public LeaderboardEntry() {
    username = new SimpleStringProperty();
    id = new SimpleStringProperty();
    rank = new SimpleIntegerProperty();
    rating = new SimpleDoubleProperty();
    gamesPlayed = new SimpleIntegerProperty();
    winLossRatio = new SimpleFloatProperty();
  }

  public static LeaderboardEntry fromLadder1v1(Ladder1v1LeaderboardEntry entry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(entry.getName());
    leaderboardEntry.setId(entry.getId());
    leaderboardEntry.setGamesPlayed(entry.getNumGames());
    leaderboardEntry.setRank(entry.getRank());
    leaderboardEntry.setRating(entry.getRating());
    leaderboardEntry.setWinLossRatio(entry.getWonGames() / (float) entry.getNumGames());
    return leaderboardEntry;
  }

  public static LeaderboardEntry fromGlobalRating(GlobalLeaderboardEntry globalLeaderboardEntry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(globalLeaderboardEntry.getName());
    leaderboardEntry.setId(globalLeaderboardEntry.getId());
    leaderboardEntry.setGamesPlayed(globalLeaderboardEntry.getNumGames());
    leaderboardEntry.setRank(globalLeaderboardEntry.getRank());
    leaderboardEntry.setRating(globalLeaderboardEntry.getRating());
    return leaderboardEntry;
  }

  public String getUsername() {
    return username.get();
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public int getRank() {
    return rank.get();
  }

  public void setRank(int rank) {
    this.rank.set(rank);
  }

  public IntegerProperty rankProperty() {
    return rank;
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
    return "Ranked1v1EntryBean{" +
        "username=" + username.get() +
        '}';
  }
}
