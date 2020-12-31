package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SearchInfoMessage extends FafServerMessage {

  @SerializedName("queue_name")
  private String queueName;
  private MatchmakingState state;

  public SearchInfoMessage() {
    super(FafServerMessageType.SEARCH_INFO);
  }

}
