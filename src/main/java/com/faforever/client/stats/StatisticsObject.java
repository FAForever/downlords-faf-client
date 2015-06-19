package com.faforever.client.stats;

import com.faforever.client.leaderboard.LeagueInfo;
import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.domain.StatisticsType;

import java.util.List;

public class StatisticsObject extends ServerObject {

  public StatisticsType type;
  public List<LeagueInfo> values;
}
