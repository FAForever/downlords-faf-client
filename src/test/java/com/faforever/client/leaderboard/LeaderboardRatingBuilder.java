package com.faforever.client.leaderboard;

public class LeaderboardRatingBuilder {
  private final LeaderboardRating leaderboardRating = new LeaderboardRating();

  public static LeaderboardRatingBuilder create() {
    return new LeaderboardRatingBuilder();
  }

  public LeaderboardRatingBuilder defaultValues() {
    mean(1500);
    deviation(500);
    numberOfGames(100);
    return this;
  }


  public LeaderboardRatingBuilder deviation(float deviation) {
    leaderboardRating.setDeviation(deviation);
    return this;
  }

  public LeaderboardRatingBuilder mean(float mean) {
    leaderboardRating.setMean(mean);
    return this;
  }

  public LeaderboardRatingBuilder numberOfGames(int numberOfGames) {
    leaderboardRating.setNumberOfGames(numberOfGames);
    return this;
  }

  public LeaderboardRating get() {
    return leaderboardRating;
  }
}