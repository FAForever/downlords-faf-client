package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;

public class MatchmakerInfoClientMessage extends ClientMessage {

  public MatchmakerInfoClientMessage() {
    super(ClientMessageType.MATCHMAKER_INFO);
  }
}
