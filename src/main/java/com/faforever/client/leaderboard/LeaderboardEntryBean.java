package com.faforever.client.leaderboard;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LeaderboardEntryBean {

  private StringProperty username;
  private IntegerProperty rank;
  private IntegerProperty rating;
  private IntegerProperty gamesPlayed;
  private FloatProperty score;
  private FloatProperty winLossRatio;
  private StringProperty division;

  public LeaderboardEntryBean() {
    username = new SimpleStringProperty();
    rank = new SimpleIntegerProperty();
    rating = new SimpleIntegerProperty();
    gamesPlayed = new SimpleIntegerProperty();
    score = new SimpleFloatProperty();
    winLossRatio = new SimpleFloatProperty();
    division = new SimpleStringProperty();
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

  public int getRating() {
    return rating.get();
  }

  public void setRating(int rating) {
    this.rating.set(rating);
  }

  public IntegerProperty ratingProperty() {
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

  public float getScore() {
    return score.get();
  }

  public void setScore(float score) {
    this.score.set(score);
  }

  public FloatProperty scoreProperty() {
    return score;
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

  public String getDivision() {
    return division.get();
  }

  public void setDivision(String division) {
    this.division.set(division);
  }

  public StringProperty divisionProperty() {
    return division;
  }


}
