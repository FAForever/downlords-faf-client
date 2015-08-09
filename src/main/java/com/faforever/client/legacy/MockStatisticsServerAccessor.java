package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockStatisticsServerAccessor implements StatisticsServerAccessor {

  @Autowired
  TaskService taskService;

  @Override
  public void requestPlayerStatistics(String username, Callback<PlayerStatistics> callback, StatisticsType type) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<PlayerStatistics>("Fetching player statistics") {
      @Override
      protected PlayerStatistics call() throws Exception {
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
    }, callback);
  }

  private RatingInfo createRatingInfo(LocalDateTime dateTime, float mean, float dev) {
    RatingInfo ratingInfo = new RatingInfo();
    ratingInfo.date = dateTime.toLocalDate();
    ratingInfo.time = dateTime.toLocalTime();
    ratingInfo.mean = mean;
    ratingInfo.dev = dev;
    return ratingInfo;
  }

}
