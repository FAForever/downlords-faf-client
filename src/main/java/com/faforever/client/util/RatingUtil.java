package com.faforever.client.util;

import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.player.LeaderboardRating;

import java.util.Optional;

public final class RatingUtil {

  private RatingUtil() {
    // Utility class
  }

  public static int roundRatingToNextLowest100(double rating) {
    double ratingToBeRounded = rating < 0 ? rating - 100 : rating;
    return (int) (ratingToBeRounded / 100) * 100;
  }

  public static Integer getRoundedLeaderboardRating(PlayerInfo player, String ratingType) {
    return getRoundedRating(getLeaderboardRating(player, ratingType));
  }

  public static Integer getRoundedLeaderboardRating(PlayerInfo player, Leaderboard leaderboard) {
    return getRoundedLeaderboardRating(player, leaderboard.technicalName());
  }

  public static int getRoundedRating(int rating) {
    return (rating + 50) / 100 * 100;
  }

  public static Integer getLeaderboardRating(PlayerInfo player, String ratingType) {
    return Optional.of(player.getLeaderboardRatings())
        .map(rating -> rating.get(ratingType))
        .map(RatingUtil::getRating)
        .orElse(0);
  }

  public static Integer getLeaderboardRating(PlayerInfo player, Leaderboard leaderboard) {
    return getLeaderboardRating(player, leaderboard.technicalName());
  }

  public static int getRating(LeaderboardRating leaderboardRating) {
    return (int) (leaderboardRating.mean() - 3f * leaderboardRating.deviation());
  }

  public static int getRating(double ratingMean, double ratingDeviation) {
    return (int) (ratingMean - 3f * ratingDeviation);
  }

  public static int getRating(LeaderboardRatingJournal ratingJournal) {
    return getRating(ratingJournal.meanBefore(), ratingJournal.deviationBefore());
  }

  public static int getRating(Rating rating) {
    return getRating(rating.mean(), rating.deviation());
  }
}
