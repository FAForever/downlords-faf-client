package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;

public class MatchCancelledMessage extends FafServerMessage {

  public MatchCancelledMessage() {
    super(FafServerMessageType.MATCH_CANCELLED);
  }
}
