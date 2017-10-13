package com.faforever.client.util;

import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.player.Player;

public final class RatingUtil {

  private RatingUtil() {
    // Utility class
  }

  public static int roundRatingToNextLowest100(double rating) {
    double ratingToBeRounded = rating < 0 ? rating - 100 : rating;
    return (int) (ratingToBeRounded / 100) * 100;
  }

  public static int getRoundedGlobalRating(Player player) {
    return getRoundedRating(getGlobalRating(player));
  }

  public static int getRoundedRating(int rating) {
    return (rating + 50) / 100 * 100;
  }

  public static int getGlobalRating(Player playerInfo) {
    return getRating(playerInfo.getGlobalRatingMean(), playerInfo.getGlobalRatingDeviation());
  }

  public static int getRating(double ratingMean, double ratingDeviation) {
    return (int) (ratingMean - 3f * ratingDeviation);
  }

  public static int getLeaderboardRating(Player player) {
    return getRating(player.getLeaderboardRatingMean(), player.getLeaderboardRatingDeviation());
  }

  public static int getGlobalRating(com.faforever.client.remote.domain.Player player) {
    return getRating(player.getGlobalRating()[0], player.getGlobalRating()[1]);
  }

  public static int getLeaderboardRating(com.faforever.client.remote.domain.Player player) {
    return getRating(player.getLadderRating()[0], player.getLadderRating()[1]);
  }

  public static int getRating(RatingHistoryDataPoint datapoint) {
    return getRating(datapoint.getMean(), datapoint.getDeviation());
  }

  public static int getRating(Rating rating) {
    return getRating(rating.getMean(), rating.getDeviation());
  }
}
