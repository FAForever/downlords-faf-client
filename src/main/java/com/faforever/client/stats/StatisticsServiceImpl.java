package com.faforever.client.stats;

import com.faforever.client.config.CacheNames;
import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

public class StatisticsServiceImpl implements StatisticsService {

  @Autowired
  StatisticsServerAccessor statisticsServerAccessor;

  @Override
  @Cacheable(value = CacheNames.STATISTICS, key = "#type + #username")
  public void getStatisticsForPlayer(StatisticsType type, String username, Callback<PlayerStatistics> callback) {
    statisticsServerAccessor.requestPlayerStatistics(username, callback, type);
  }
}
