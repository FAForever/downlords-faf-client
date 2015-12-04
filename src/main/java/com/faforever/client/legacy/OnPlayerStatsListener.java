package com.faforever.client.legacy;

import com.faforever.client.stats.PlayerStatisticsMessage;

public interface OnPlayerStatsListener {

  void onPlayerStats(PlayerStatisticsMessage playerStatisticsMessage);
}
