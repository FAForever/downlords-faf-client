package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;

public class RankedMatchNotification extends ServerMessage {

  // TODO add javadoc as soon as you know what that means :P
  public boolean potential;

  public RankedMatchNotification(boolean potential) {
    super(ServerMessageType.MATCHMAKER_INFO);
    this.potential = potential;
  }
}
