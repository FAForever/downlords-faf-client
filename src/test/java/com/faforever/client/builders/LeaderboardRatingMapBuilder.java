package com.faforever.client.builders;

import com.faforever.client.player.LeaderboardRating;
import org.instancio.Instancio;

import java.util.HashMap;
import java.util.Map;

import static org.instancio.Select.field;

public class LeaderboardRatingMapBuilder {
  private final Map<String, LeaderboardRating> leaderboardRatingMap = new HashMap<>();

  public static LeaderboardRatingMapBuilder create() {
    return new LeaderboardRatingMapBuilder();
  }

  public LeaderboardRatingMapBuilder defaultValues() {
    put("ladder_1v1", Instancio.create(LeaderboardRating.class));
    put("global", Instancio.create(LeaderboardRating.class));
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, LeaderboardRating leaderboardRating) {
    leaderboardRatingMap.put(ratingType, leaderboardRating);
    return this;
  }

  public LeaderboardRatingMapBuilder put(String ratingType, float mean, float deviation, int numGames) {
    leaderboardRatingMap.put(ratingType, Instancio.of(LeaderboardRating.class)
                                                  .set(field(LeaderboardRating::mean), mean)
                                                  .set(field(LeaderboardRating::deviation), deviation)
                                                  .set(field(LeaderboardRating::numberOfGames), numGames)
                                                  .create());
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
