package com.faforever.client.leaderboard;

public class RatingDistribution implements Comparable<RatingDistribution> {

  private int maxRating;
  private int players;

  public RatingDistribution(int maxRating) {
    this.maxRating = maxRating;
  }

  public int getPlayers() {
    return players;
  }

  public void setPlayers(int players) {
    this.players = players;
  }

  public void incrementPlayers() {
    players++;
  }

  @Override
  public int compareTo(RatingDistribution o) {
    return Integer.compare(maxRating, o.getMaxRating());
  }

  public int getMaxRating() {
    return maxRating;
  }

  public void setMaxRating(int maxRating) {
    this.maxRating = maxRating;
  }
}
