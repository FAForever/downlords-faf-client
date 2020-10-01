package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchCancelledMessage extends FafServerMessage {

  public MatchCancelledMessage() {
    super(FafServerMessageType.MATCH_CANCELLED);
  }
}
