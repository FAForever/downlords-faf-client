package com.faforever.client.stats;

import com.faforever.client.remote.domain.StatisticsType;

import java.util.concurrent.CompletionStage;

public interface StatisticsService {

  CompletionStage<PlayerStatisticsMessage> getStatisticsForPlayer(StatisticsType type, String username);

}
