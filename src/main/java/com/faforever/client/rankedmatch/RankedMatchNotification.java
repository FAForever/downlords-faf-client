package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.domain.ServerObjectType;

public class RankedMatchNotification extends ServerObject {

  // TODO add javadoc as soon as you know what that means :P
  public boolean potential;

  public RankedMatchNotification(boolean potential) {
    command = ServerObjectType.MATCHMAKER_INFO.getString();
    this.potential = potential;
  }
}
