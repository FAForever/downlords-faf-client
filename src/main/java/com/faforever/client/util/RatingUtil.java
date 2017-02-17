package com.faforever.client.util;

import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.player.Player;

import java.util.regex.Pattern;

public final class RatingUtil {

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  private RatingUtil() {
    // Utility class
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

  public static int getRating(float ratingMean, float ratingDeviation) {
    return (int) (ratingMean - 3 * ratingDeviation);
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
}
