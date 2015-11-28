package com.faforever.client.stats;

import com.faforever.client.config.CacheNames;
import com.faforever.client.legacy.StatisticsServerAccessor;
import com.faforever.client.legacy.domain.StatisticsType;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

public class StatisticsServiceImpl implements StatisticsService {

  @Resource
  StatisticsServerAccessor statisticsServerAccessor;

  @Override
  @Cacheable(value = CacheNames.STATISTICS, key = "#type + #username")
  public CompletableFuture<PlayerStatisticsMessageLobby> getStatisticsForPlayer(StatisticsType type, String username) {
    return statisticsServerAccessor.requestPlayerStatistics(type, username);
  }
}
