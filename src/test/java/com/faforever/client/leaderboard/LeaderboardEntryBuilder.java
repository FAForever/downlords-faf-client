package com.faforever.client.leaderboard;

public class LeaderboardEntryBuilder {
  private final LeaderboardEntry leaderboardEntry = new LeaderboardEntry();

  public static LeaderboardEntryBuilder create() {
    return new LeaderboardEntryBuilder();
  }

  public LeaderboardEntryBuilder defaultValues() {
    username("junit");
    rating(1000);
    gamesPlayed(100);
    winLossRatio(.5f);
    leaderboard(LeaderboardBuilder.create().defaultValues().get());
    return this;
  }

  public LeaderboardEntryBuilder username(String username) {
    leaderboardEntry.setUsername(username);
    return this;
  }

  public LeaderboardEntryBuilder rating(double rating) {
    leaderboardEntry.setRating(rating);
    return this;
  }

  public LeaderboardEntryBuilder gamesPlayed(int gamesPlayed) {
    leaderboardEntry.setGamesPlayed(gamesPlayed);
    return this;
  }

  public LeaderboardEntryBuilder winLossRatio(float winLossRatio) {
    leaderboardEntry.setWinLossRatio(winLossRatio);
    return this;
  }

  public LeaderboardEntryBuilder leaderboard(Leaderboard leaderboard) {
    leaderboardEntry.setLeaderboard(leaderboard);
    return this;
  }

  public LeaderboardEntry get() {
    return leaderboardEntry;
  }
}
