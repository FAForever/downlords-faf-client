package com.faforever.client.leaderboard;

import com.faforever.client.stats.StatisticsObject;

import java.util.List;

public class LeagueResponse extends StatisticsObject {

  private List<LeagueInfo> values;

  public List<LeagueInfo> getValues() {
    return values;
  }

  public void setValues(List<LeagueInfo> values) {
    this.values = values;
  }
}
