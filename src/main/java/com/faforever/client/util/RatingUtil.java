package com.faforever.client.util;

import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.player.Player;

import java.util.Optional;

public final class RatingUtil {

  private RatingUtil() {
    // Utility class
  }

  public static int roundRatingToNextLowest100(double rating) {
    double ratingToBeRounded = rating < 0 ? rating - 100 : rating;
    return (int) (ratingToBeRounded / 100) * 100;
  }

  public static Integer getRoundedLeaderboardRating(Player player, String ratingType) {
    return getRoundedRating(getLeaderboardRating(player, ratingType));
  }

  public static Integer getRoundedLeaderboardRating(Player player, Leaderboard leaderboard) {
    return getRoundedLeaderboardRating(player, leaderboard.getTechnicalName());
  }

  public static int getRoundedRating(int rating) {
    return (rating + 50) / 100 * 100;
  }

  public static Integer getLeaderboardRating(Player player, String ratingType) {
    return Optional.of(player.getLeaderboardRatings())
        .map(rating -> rating.get(ratingType))
        .map(RatingUtil::getRating)
        .orElse(0);
  }

  public static Integer getLeaderboardRating(Player player, Leaderboard leaderboard) {
    return getLeaderboardRating(player, leaderboard.getTechnicalName());
  }

  public static int getRating(LeaderboardRating leaderboardRating) {
    return (int) (leaderboardRating.getMean() - 3f * leaderboardRating.getDeviation());
  }

  public static int getRating(double ratingMean, double ratingDeviation) {
    return (int) (ratingMean - 3f * ratingDeviation);
  }

  public static int getRating(RatingHistoryDataPoint datapoint) {
    return getRating(datapoint.getMean(), datapoint.getDeviation());
  }

  public static int getRating(Rating rating) {
    return getRating(rating.getMean(), rating.getDeviation());
  }
}
