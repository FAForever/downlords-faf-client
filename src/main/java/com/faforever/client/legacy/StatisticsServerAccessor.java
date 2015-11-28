package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatisticsMessageLobby;

import java.util.concurrent.CompletableFuture;

public interface StatisticsServerAccessor {

  CompletableFuture<PlayerStatisticsMessageLobby> requestPlayerStatistics(StatisticsType type, String username);

}
