package com.faforever.client.remote.domain;

/**
 * Rating as received from the FAF server in a player message.
 */
public class LeaderboardRating {

  private Integer numberOfGames;
  private float[] rating;

  public float[] getRating() {
    return rating;
  }

  public void setRating(float[] rating) {
    this.rating = rating;
  }


  public Integer getNumberOfGames() {
    return numberOfGames;
  }

  public void setNumberOfGames(Integer numberOfGames) {
    this.numberOfGames = numberOfGames;
  }
}
