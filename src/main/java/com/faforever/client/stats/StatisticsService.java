package com.faforever.client.stats;

import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.util.Callback;

public interface StatisticsService {

  void getStatisticsForPlayer(StatisticsType type, String username, Callback<PlayerStatistics> callback);
}
