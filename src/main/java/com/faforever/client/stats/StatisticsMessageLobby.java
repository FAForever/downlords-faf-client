package com.faforever.client.stats;

import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;

public class StatisticsMessageLobby extends FafServerMessage {

  private StatisticsType type;

  public StatisticsMessageLobby() {
    super(FafServerMessageType.STATS);
  }

  public StatisticsType getStatisticsType() {
    return type;
  }

  public void setStatisticsType(StatisticsType type) {
    this.type = type;
  }
}
