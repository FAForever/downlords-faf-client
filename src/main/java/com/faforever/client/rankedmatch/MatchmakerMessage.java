package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;

public class MatchmakerMessage extends FafServerMessage {

  /**
   * Flag indicating whether a potential opponent is available or not.
   */
  public boolean potential;

  public MatchmakerMessage() {
    super(FafServerMessageType.MATCHMAKER_INFO);
  }

  public boolean isPotential() {
    return potential;
  }

  public void setPotential(boolean potential) {
    this.potential = potential;
  }
}
