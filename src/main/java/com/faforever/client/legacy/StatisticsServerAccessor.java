package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatistics;

import java.util.concurrent.CompletableFuture;

public interface StatisticsServerAccessor {

  CompletableFuture<PlayerStatistics> requestPlayerStatistics(String username, StatisticsType type);

}
