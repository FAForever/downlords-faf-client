package com.faforever.client.stats;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.StatisticsType;

public class StatisticsMessage extends FafServerMessage {

  private StatisticsType type;

  public StatisticsMessage() {
    super(FafServerMessageType.STATS);
  }

  public StatisticsType getStatisticsType() {
    return type;
  }

  public void setStatisticsType(StatisticsType type) {
    this.type = type;
  }
}
