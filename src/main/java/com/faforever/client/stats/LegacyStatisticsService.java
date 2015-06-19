package com.faforever.client.stats;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class LegacyStatisticsService implements StatisticsService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void getStatisticsForPlayer(String username, Callback<PlayerStatistics> callback) {
    serverAccessor.requestPlayerStatistics(username, callback);
  }
}
