package com.faforever.client.stats;

import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class StatisticsServiceImpl implements StatisticsService {

  @Autowired
  StatisticsServerAccessor statisticsServerAccessor;

  @Override
  public void getStatisticsForPlayer(StatisticsType type, String username, Callback<PlayerStatistics> callback) {
    statisticsServerAccessor.requestPlayerStatistics(username, callback, type);
  }
}
