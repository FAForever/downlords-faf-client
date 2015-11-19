package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.MEDIUM;

public class MockStatisticsServerAccessor implements StatisticsServerAccessor {

  @Autowired
  TaskService taskService;

  @Override
  public CompletableFuture<PlayerStatistics> requestPlayerStatistics(StatisticsType type, String username) {
    return taskService.submitTask(new AbstractPrioritizedTask<PlayerStatistics>(MEDIUM) {
      @Override
      protected PlayerStatistics call() throws Exception {
        updateTitle("Fetching player statistics");

        ArrayList<RatingInfo> ratings = new ArrayList<>();
        for (int day = 0; day < 90; day++) {
          LocalDateTime localDateTime = LocalDateTime.now().plusDays(day);
          float mean = (float) (1500 + Math.sin(day) * 300);
          float dev = 60;
          ratings.add(createRatingInfo(localDateTime, mean, dev));
        }

        PlayerStatistics playerStatistics = new PlayerStatistics();
        playerStatistics.setStatisticsType(StatisticsType.STATS);
        playerStatistics.setValues(ratings);
        return playerStatistics;
      }
    });
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
