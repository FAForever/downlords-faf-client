package com.faforever.client.rankedmatch;

import com.faforever.client.rankedmatch.MatchmakerInfoMessage.MatchmakerQueue.QueueName;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MatchFoundMessage extends FafServerMessage {

  @SerializedName("queue")
  private QueueName queueName;

  public MatchFoundMessage() {
    super(FafServerMessageType.MATCH_FOUND);
  }
}
