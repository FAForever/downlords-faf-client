package com.faforever.client.stats;

import com.faforever.client.util.Callback;

public interface StatisticsService {

  void getStatisticsForPlayer(String username, Callback<PlayerStatistics> callback);
}
