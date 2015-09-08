package com.faforever.client.leaderboard;

import com.faforever.client.stats.StatisticsMessage;

import java.util.List;

public class LeagueResponse extends StatisticsMessage {

  private List<LeagueInfo> values;

  public List<LeagueInfo> getValues() {
    return values;
  }

  public void setValues(List<LeagueInfo> values) {
    this.values = values;
  }
}
