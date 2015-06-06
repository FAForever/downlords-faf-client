package com.faforever.client.leaderboard;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LadderInfoBean {
  private StringProperty username;
  private IntegerProperty rank;
  private IntegerProperty rating;
  private IntegerProperty gamesPlayed;
  private FloatProperty score;
  private FloatProperty winLossRatio;
  private StringProperty division;

  public LadderInfoBean() {
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

  public StringProperty usernameProperty() {
    return username;
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public int getRank() {
    return rank.get();
  }

  public IntegerProperty rankProperty() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank.set(rank);
  }

  public int getRating() {
    return rating.get();
  }

  public IntegerProperty ratingProperty() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating.set(rating);
  }

  public int getGamesPlayed() {
    return gamesPlayed.get();
  }

  public IntegerProperty gamesPlayedProperty() {
    return gamesPlayed;
  }

  public void setGamesPlayed(int gamesPlayed) {
    this.gamesPlayed.set(gamesPlayed);
  }

  public float getScore() {
    return score.get();
  }

  public FloatProperty scoreProperty() {
    return score;
  }

  public void setScore(float score) {
    this.score.set(score);
  }

  public float getWinLossRatio() {
    return winLossRatio.get();
  }

  public FloatProperty winLossRatioProperty() {
    return winLossRatio;
  }

  public void setWinLossRatio(float winLossRatio) {
    this.winLossRatio.set(winLossRatio);
  }

  public String getDivision() {
    return division.get();
  }

  public StringProperty divisionProperty() {
    return division;
  }

  public void setDivision(String division) {
    this.division.set(division);
  }
}
