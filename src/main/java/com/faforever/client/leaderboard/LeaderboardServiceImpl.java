package com.faforever.client.leaderboard;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.util.RatingUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LeaderboardServiceImpl implements LeaderboardService {

  private static final int RATING_INCREMENTS = 100;

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> getLeaderboardEntries() {
    return lobbyServerAccessor.requestLeaderboardEntries();
  }

  @Override
  public CompletableFuture<List<RatingDistribution>> getRatingDistributions() {
    CompletableFuture<List<RatingDistribution>> future = new CompletableFuture<>();
    getLeaderboardEntries().thenAcceptAsync(result -> future.complete(calculateRatingDistributions(result)));
    return future;
  }

  @NotNull
  private List<RatingDistribution> calculateRatingDistributions(List<LeaderboardEntryBean> leaderboardEntryBeans) {
    Map<Integer, RatingDistribution> result = new HashMap<>();
    for (LeaderboardEntryBean leaderboardEntryBean : leaderboardEntryBeans) {
      int roundedRating = RatingUtil.getRoundedRating(leaderboardEntryBean.getRating());
      if (!result.containsKey(roundedRating)) {
        result.put(roundedRating, new RatingDistribution(roundedRating));
      }
      result.get(roundedRating).incrementPlayers();
    }
    ArrayList<RatingDistribution> ratingDistributions = new ArrayList<>(result.values());
    Collections.sort(ratingDistributions);
    return ratingDistributions;
  }
}
