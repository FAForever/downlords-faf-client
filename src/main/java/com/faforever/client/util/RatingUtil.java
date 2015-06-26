package com.faforever.client.util;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.stats.RatingInfo;

public final class RatingUtil {

  private RatingUtil() {
    // Utility class
  }

  public static int getRating(PlayerInfo playerInfo) {
    return getRating(playerInfo.ratingMean, playerInfo.ratingDeviation);
  }


  public static int getRating(RatingInfo ratingInfo) {
    return getRating(ratingInfo.mean, ratingInfo.dev);
  }

  public static int getRating(PlayerInfoBean playerInfo) {
    return getRating(playerInfo.getMean(), playerInfo.getDeviation());
  }

  private static int getRating(float ratingMean, float ratingDeviation) {
    return (int) (ratingMean - 3 * ratingDeviation);
  }
}
