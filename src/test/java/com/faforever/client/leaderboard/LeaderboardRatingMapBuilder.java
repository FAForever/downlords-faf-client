package com.faforever.client.leaderboard;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardRatingMapBuilder {
  private final Map<String, LeaderboardRating> leaderboardRatingMap = new HashMap<>();

  public static LeaderboardRatingMapBuilder create() {
    return new LeaderboardRatingMapBuilder();
  }

  public LeaderboardRatingMapBuilder defaultValues() {
    put("ladder_1v1", LeaderboardRatingBuilder.create().defaultValues().get());
    put("global", LeaderboardRatingBuilder.create().defaultValues().get());
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, LeaderboardRating leaderboardRating) {
    leaderboardRatingMap.put(ratingType, leaderboardRating);
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, float mean, float deviation, int numGames) {
    leaderboardRatingMap.put(ratingType, LeaderboardRatingBuilder.create().mean(mean).deviation(deviation).numberOfGames(numGames).get());
    return this;
  }

  public LeaderboardRatingMapBuilder replace(Map<String, LeaderboardRating> leaderboardRatingMap) {
    this.leaderboardRatingMap.clear();
    this.leaderboardRatingMap.putAll(leaderboardRatingMap);
    return this;
  }

  public Map<String, LeaderboardRating> get() {
    return leaderboardRatingMap;
  }
}
