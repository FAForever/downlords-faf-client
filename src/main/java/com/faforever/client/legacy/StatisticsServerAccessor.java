package com.faforever.client.legacy;

import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.util.Callback;

public interface StatisticsServerAccessor {

  void requestPlayerStatistics(String username, Callback<PlayerStatistics> callback);

}
