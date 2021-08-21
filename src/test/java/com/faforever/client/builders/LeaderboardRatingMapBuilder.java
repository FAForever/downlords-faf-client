package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardRatingBean;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardRatingMapBuilder {
  private final Map<String, LeaderboardRatingBean> leaderboardRatingMap = new HashMap<>();

  public static LeaderboardRatingMapBuilder create() {
    return new LeaderboardRatingMapBuilder();
  }

  public LeaderboardRatingMapBuilder defaultValues() {
    put("ladder_1v1", LeaderboardRatingBeanBuilder.create().defaultValues().get());
    put("global", LeaderboardRatingBeanBuilder.create().defaultValues().get());
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, LeaderboardRatingBean leaderboardRating) {
    leaderboardRatingMap.put(ratingType, leaderboardRating);
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, float mean, float deviation, int numGames) {
    leaderboardRatingMap.put(ratingType, LeaderboardRatingBeanBuilder.create().mean(mean).deviation(deviation).numberOfGames(numGames).get());
    return this;
  }

  public LeaderboardRatingMapBuilder replace(Map<String, LeaderboardRatingBean> leaderboardRatingMap) {
    this.leaderboardRatingMap.clear();
    this.leaderboardRatingMap.putAll(leaderboardRatingMap);
    return this;
  }

  public Map<String, LeaderboardRatingBean> get() {
    return leaderboardRatingMap;
  }
}
