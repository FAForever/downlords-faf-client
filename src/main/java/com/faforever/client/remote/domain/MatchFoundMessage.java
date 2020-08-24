package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchFoundMessage extends FafServerMessage {

  private String queue;

  public MatchFoundMessage() {
    super(FafServerMessageType.MATCH_FOUND);
  }
}
