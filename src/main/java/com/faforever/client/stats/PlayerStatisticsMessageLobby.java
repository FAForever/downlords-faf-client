package com.faforever.client.stats;

import java.util.List;

public class PlayerStatisticsMessageLobby extends StatisticsMessageLobby {

  private List<RatingInfo> values;
  private String player;

  public List<RatingInfo> getValues() {
    return values;
  }

  public void setValues(List<RatingInfo> values) {
    this.values = values;
  }

  public String getPlayer() {
    return player;
  }

  public void setPlayer(String player) {
    this.player = player;
  }
}
