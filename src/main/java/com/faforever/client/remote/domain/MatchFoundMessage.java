package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchFoundMessage extends FafServerMessage {

  @SerializedName("queue_name")
  private String queueName;

  public MatchFoundMessage() {
    super(FafServerMessageType.MATCH_FOUND);
  }
}
