package com.faforever.client.leaderboard;

import com.faforever.client.remote.FafService;
import com.faforever.client.util.RatingUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LeaderboardServiceImpl implements LeaderboardService {

  @Resource
  FafService fafService;

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

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> getLeaderboardEntries() {
    return fafService.requestLeaderboardEntries();
  }

  @Override
  public CompletableFuture<List<RatingDistribution>> getRatingDistributions() {
    return getLeaderboardEntries().thenApply(this::calculateRatingDistributions);
  }

  @Override
  public CompletableFuture<LeaderboardEntryBean> getEntryForPlayer(String username) {
    // TODO server side support would be nice
    return getLeaderboardEntries().thenApply(leaderboardEntryBeans -> {
      for (LeaderboardEntryBean leaderboardEntryBean : leaderboardEntryBeans) {
        if (username.equals(leaderboardEntryBean.getUsername())) {
          return leaderboardEntryBean;
        }
      }
      return null;
    });
  }


}
