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

  public static int getRating(PlayerInfo playerInfo) {
    return getRating(playerInfo.ratingMean, playerInfo.ratingDeviation);
  }

  private static int getRating(float ratingMean, float ratingDeviation) {
    return (int) (ratingMean - 3 * ratingDeviation);
  }

  public static int getRating(RatingInfo ratingInfo) {
    return getRating(ratingInfo.mean, ratingInfo.dev);
  }

  public static int getRating(PlayerInfoBean playerInfo) {
    return getRating(playerInfo.getMean(), playerInfo.getDeviation());
  }

  public static String extractRating(String title) {
    Matcher matcher = RATING_PATTERN.matcher(title);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
