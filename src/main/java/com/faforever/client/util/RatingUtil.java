package com.faforever.client.util;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.domain.Player;
import com.faforever.client.stats.RatingInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RatingUtil {

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  private RatingUtil() {
    // Utility class
  }

  public static int getRoundedGlobalRating(PlayerInfoBean player) {
    return getRoundedRating(getGlobalRating(player));
  }

  public static int getRoundedRating(int rating) {
    return (rating + 50) / 100 * 100;
  }

  public static int getGlobalRating(PlayerInfoBean playerInfo) {
    return getRating(playerInfo.getGlobalRatingMean(), playerInfo.getGlobalRatingDeviation());
  }

  private static int getRating(float ratingMean, float ratingDeviation) {
    return (int) (ratingMean - 3 * ratingDeviation);
  }

  public static int getRoundedLeaderboardRating(PlayerInfoBean player) {
    return getRoundedRating(getLeaderboardRating(player));
  }

  public static int getLeaderboardRating(PlayerInfoBean playerInfoBean) {
    return getRating(playerInfoBean.getLeaderboardRatingMean(), playerInfoBean.getLeaderboardRatingDeviation());
  }

  public static int getGlobalRating(Player player) {
    return getRating(player.getRatingMean(), player.getRatingDeviation());
  }

  public static int getLeaderboardRating(Player player) {
    return getRating(player.getLadderRatingMean(), player.getLadderRatingDeviation());
  }

  public static int getRating(RatingInfo ratingInfo) {
    return getRating(ratingInfo.getMean(), ratingInfo.getDev());
  }

  public static String extractRating(String title) {
    Matcher matcher = RATING_PATTERN.matcher(title);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
