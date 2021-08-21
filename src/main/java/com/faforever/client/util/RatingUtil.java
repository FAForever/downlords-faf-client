package com.faforever.client.util;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.PlayerBean;

import java.util.Optional;

public final class RatingUtil {

  private RatingUtil() {
    // Utility class
  }

  public static int roundRatingToNextLowest100(double rating) {
    double ratingToBeRounded = rating < 0 ? rating - 100 : rating;
    return (int) (ratingToBeRounded / 100) * 100;
  }

  public static Integer getRoundedLeaderboardRating(PlayerBean player, String ratingType) {
    return getRoundedRating(getLeaderboardRating(player, ratingType));
  }

  public static Integer getRoundedLeaderboardRating(PlayerBean player, LeaderboardBean leaderboard) {
    return getRoundedLeaderboardRating(player, leaderboard.getTechnicalName());
  }

  public static int getRoundedRating(int rating) {
    return (rating + 50) / 100 * 100;
  }

  public static Integer getLeaderboardRating(PlayerBean player, String ratingType) {
    return Optional.of(player.getLeaderboardRatings())
        .map(rating -> rating.get(ratingType))
        .map(RatingUtil::getRating)
        .orElse(0);
  }

  public static Integer getLeaderboardRating(PlayerBean player, LeaderboardBean leaderboard) {
    return getLeaderboardRating(player, leaderboard.getTechnicalName());
  }

  public static int getRating(LeaderboardRatingBean leaderboardRating) {
    return (int) (leaderboardRating.getMean() - 3f * leaderboardRating.getDeviation());
  }

  public static int getRating(double ratingMean, double ratingDeviation) {
    return (int) (ratingMean - 3f * ratingDeviation);
  }

  public static int getRating(LeaderboardRatingJournalBean ratingJournal) {
    return getRating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore());
  }

  public static int getRating(Rating rating) {
    return getRating(rating.getMean(), rating.getDeviation());
  }
}
