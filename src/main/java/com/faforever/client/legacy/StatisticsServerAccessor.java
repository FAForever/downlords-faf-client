package com.faforever.client.legacy;

import com.faforever.client.remote.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatisticsMessage;

import java.util.concurrent.CompletableFuture;

public interface StatisticsServerAccessor {

  CompletableFuture<PlayerStatisticsMessage> requestPlayerStatistics(StatisticsType type, String username);

}
