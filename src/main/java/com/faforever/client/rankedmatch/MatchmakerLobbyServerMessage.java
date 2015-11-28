package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;

public class MatchmakerLobbyServerMessage extends FafServerMessage {

  /**
   * Flag indicating whether a potential opponent is available or not.
   */
  public boolean potential;

  public MatchmakerLobbyServerMessage() {
    super(FafServerMessageType.MATCHMAKER_INFO);
  }

  public void setPotential(boolean potential) {
    this.potential = potential;
  }
}
