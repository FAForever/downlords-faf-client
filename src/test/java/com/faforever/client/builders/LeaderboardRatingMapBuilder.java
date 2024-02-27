package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardRatingBean;
import org.instancio.Instancio;

import java.util.HashMap;
import java.util.Map;

import static org.instancio.Select.field;

public class LeaderboardRatingMapBuilder {
  private final Map<String, LeaderboardRatingBean> leaderboardRatingMap = new HashMap<>();

  public static LeaderboardRatingMapBuilder create() {
    return new LeaderboardRatingMapBuilder();
  }

  public LeaderboardRatingMapBuilder defaultValues() {
    put("ladder_1v1", Instancio.create(LeaderboardRatingBean.class));
    put("global", Instancio.create(LeaderboardRatingBean.class));
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, LeaderboardRatingBean leaderboardRating) {
    leaderboardRatingMap.put(ratingType, leaderboardRating);
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, float mean, float deviation, int numGames) {
    leaderboardRatingMap.put(ratingType, Instancio.of(LeaderboardRatingBean.class)
                                                  .set(field(LeaderboardRatingBean::mean), mean)
                                                  .set(field(LeaderboardRatingBean::deviation), deviation)
                                                  .set(field(LeaderboardRatingBean::numberOfGames), numGames)
                                                  .create());
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
