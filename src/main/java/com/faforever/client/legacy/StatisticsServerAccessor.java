package com.faforever.client.legacy;

import com.faforever.client.remote.domain.StatisticsType;
import com.faforever.client.stats.PlayerStatisticsMessage;

import java.util.concurrent.CompletionStage;

public interface StatisticsServerAccessor {

  CompletionStage<PlayerStatisticsMessage> requestPlayerStatistics(StatisticsType type, String username);

}
