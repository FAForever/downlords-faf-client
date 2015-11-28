package com.faforever.client.legacy;

import com.faforever.client.stats.PlayerStatisticsMessageLobby;

public interface OnPlayerStatsListener {

  void onPlayerStats(PlayerStatisticsMessageLobby playerStatisticsMessage);
}
