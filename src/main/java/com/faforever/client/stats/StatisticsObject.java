package com.faforever.client.stats;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;

public class StatisticsObject extends ServerMessage {

  private StatisticsType type;

  public StatisticsObject() {
    super(ServerMessageType.STATS);
  }

  public StatisticsType getStatisticsType() {
    return type;
  }

  public void setStatisticsType(StatisticsType type) {
    this.type = type;
  }
}
