package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;

public class RankedMatchNotification extends ServerMessage {

  /**
   * Flag indicating whether a potential opponent is available or not.
   */
  public boolean potential;

  public RankedMatchNotification(boolean potential) {
    super(ServerMessageType.MATCHMAKER_INFO);
    this.potential = potential;
  }
}
