package com.faforever.client.api;

import com.google.api.client.util.Key;

public class LeaderboardEntry {

  @Key
  private String id;
  @Key
  private String login;
  @Key
  private float mean;
  @Key
  private float deviation;
  @Key("num_games")
  private int numGames;
  @Key("won_games")
  private int wonGames;
  @Key("is_active")
  private boolean isActive;
  @Key("rating")
  private int rating;
  @Key("ranking")
  private int ranking;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public float getMean() {
    return mean;
  }

  public void setMean(float mean) {
    this.mean = mean;
  }

  public float getDeviation() {
    return deviation;
  }

  public void setDeviation(float deviation) {
    this.deviation = deviation;
  }

  public int getNumGames() {
    return numGames;
  }

  public void setNumGames(int numGames) {
    this.numGames = numGames;
  }

  public int getWonGames() {
    return wonGames;
  }

  public void setWonGames(int wonGames) {
    this.wonGames = wonGames;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public int getRanking() {
    return ranking;
  }

  public void setRanking(int ranking) {
    this.ranking = ranking;
  }
}
