package com.faforever.client.leaderboard;

import com.faforever.client.api.dto.DivisionLeaderboardEntry;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LeaderboardEntry {

  private StringProperty username;
  private IntegerProperty rank;
  private DoubleProperty rating;
  private IntegerProperty gamesPlayed;
  private FloatProperty winLossRatio;
  private IntegerProperty score;
  private IntegerProperty majorDivisionIndex;
  private IntegerProperty subDivisionIndex;

  public LeaderboardEntry() {
    username = new SimpleStringProperty();
    rank = new SimpleIntegerProperty();
    rating = new SimpleDoubleProperty();
    gamesPlayed = new SimpleIntegerProperty();
    winLossRatio = new SimpleFloatProperty();
    score = new SimpleIntegerProperty();
    majorDivisionIndex = new SimpleIntegerProperty();
    subDivisionIndex = new SimpleIntegerProperty();
  }

  public static LeaderboardEntry fromLadder1v1(Ladder1v1LeaderboardEntry entry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(entry.getName());
    leaderboardEntry.setGamesPlayed(entry.getNumGames());
    leaderboardEntry.setRank(entry.getRank());
    leaderboardEntry.setRating(entry.getRating());
    leaderboardEntry.setWinLossRatio(entry.getWonGames() / (float) entry.getNumGames());
    return leaderboardEntry;
  }

  public static LeaderboardEntry fromGlobalRating(GlobalLeaderboardEntry globalLeaderboardEntry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(globalLeaderboardEntry.getName());
    leaderboardEntry.setGamesPlayed(globalLeaderboardEntry.getNumGames());
    leaderboardEntry.setRank(globalLeaderboardEntry.getRank());
    leaderboardEntry.setRating(globalLeaderboardEntry.getRating());
    return leaderboardEntry;
  }

  public static LeaderboardEntry fromDivision(DivisionLeaderboardEntry divisionLeaderboardEntry) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(divisionLeaderboardEntry.getName());
    leaderboardEntry.setGamesPlayed(divisionLeaderboardEntry.getNumGames());
    leaderboardEntry.setRank(divisionLeaderboardEntry.getRank());
    leaderboardEntry.setScore(divisionLeaderboardEntry.getScore());
    leaderboardEntry.setMajorDivisionIndex(divisionLeaderboardEntry.getMajorDivisionIndex());
    leaderboardEntry.setSubDivisionIndex(divisionLeaderboardEntry.getSubDivisionIndex());
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

  public int getScore() {
    return score.get();
  }

  public void setScore(int score) {
    this.score.set(score);
  }

  public IntegerProperty scoreProperty() {
    return score;
  }

  public int getMajorDivisionIndex() {
    return majorDivisionIndex.get();
  }

  public void setMajorDivisionIndex(int majorDivisionIndex) {
    this.majorDivisionIndex.set(majorDivisionIndex);
  }

  public IntegerProperty majorDivisionIndexProperty() {
    return majorDivisionIndex;
  }

  public int getSubDivisionIndex() {
    return subDivisionIndex.get();
  }

  public void setSubDivisionIndex(int subDivisionIndex) {
    this.subDivisionIndex.set(subDivisionIndex);
  }

  public IntegerProperty subDivisionIndexProperty() {
    return subDivisionIndex;
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
