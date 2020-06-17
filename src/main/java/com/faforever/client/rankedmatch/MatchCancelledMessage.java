package com.faforever.client.rankedmatch;

import com.faforever.client.rankedmatch.MatchmakerInfoMessage.MatchmakerQueue.QueueName;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.google.gson.annotations.SerializedName;

public class MatchCancelledMessage extends FafServerMessage {

  public MatchCancelledMessage() {
    super(FafServerMessageType.MATCH_FOUND);
  }
}
