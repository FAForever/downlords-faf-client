package com.faforever.client.legacy;

import com.faforever.client.remote.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatisticsMessage;
import com.faforever.client.stats.RatingInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockStatisticsServerAccessor implements StatisticsServerAccessor {

  @Override
  public CompletionStage<PlayerStatisticsMessage> requestPlayerStatistics(StatisticsType type, String username) {
    ArrayList<RatingInfo> ratings = new ArrayList<>();
    for (int day = 0; day < 90; day++) {
      LocalDateTime localDateTime = LocalDateTime.now().plusDays(day);
      float mean = (float) (1500 + Math.sin(day) * 300);
      float dev = 60;
      ratings.add(createRatingInfo(localDateTime, mean, dev));
    }

    PlayerStatisticsMessage playerStatisticsMessage = new PlayerStatisticsMessage();
    playerStatisticsMessage.setStatisticsType(StatisticsType.STATS);
    playerStatisticsMessage.setValues(ratings);
    return CompletableFuture.completedFuture(playerStatisticsMessage);
  }

  private RatingInfo createRatingInfo(LocalDateTime dateTime, float mean, float dev) {
    RatingInfo ratingInfo = new RatingInfo();
    ratingInfo.setDate(dateTime.toLocalDate());
    ratingInfo.setTime(dateTime.toLocalTime());
    ratingInfo.setMean(mean);
    ratingInfo.setDev(dev);
    return ratingInfo;
  }
}
