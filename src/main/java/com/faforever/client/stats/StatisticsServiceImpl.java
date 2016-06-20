package com.faforever.client.stats;

import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.remote.domain.StatisticsType;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

public class StatisticsServiceImpl implements StatisticsService {

  @Resource
  StatisticsServerAccessor statisticsServerAccessor;

  @Override
  public CompletableFuture<PlayerStatisticsMessage> getStatisticsForPlayer(StatisticsType type, String username) {
    return statisticsServerAccessor.requestPlayerStatistics(type, username);
  }
}
