package com.faforever.client.util;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.stats.RatingInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RatingUtil {

  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  private RatingUtil() {
    // Utility class
  }

  public static int getGlobalRating(PlayerInfo playerInfo) {
    return getRating(playerInfo.getRatingMean(), playerInfo.getRatingDeviation());
  }

  private static int getRating(float ratingMean, float ratingDeviation) {
    return (int) (ratingMean - 3 * ratingDeviation);
  }

  public static int getLadderRating(PlayerInfo playerInfo) {
    return getRating(playerInfo.getLadderRatingMean(), playerInfo.getLadderRatingDeviation());
  }

  public static int getLadderRating(PlayerInfoBean playerInfoBean) {
    return getRating(playerInfoBean.getLeaderboardRatingMean(), playerInfoBean.getLeaderboardRatingDeviation());
  }

  public static int getGlobalRating(RatingInfo ratingInfo) {
    return getRating(ratingInfo.getMean(), ratingInfo.getDev());
  }

  public static int getGlobalRating(PlayerInfoBean playerInfo) {
    return getRating(playerInfo.getGlobalRatingMean(), playerInfo.getGlobalRatingDeviation());
  }

  public static String extractRating(String title) {
    Matcher matcher = RATING_PATTERN.matcher(title);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
