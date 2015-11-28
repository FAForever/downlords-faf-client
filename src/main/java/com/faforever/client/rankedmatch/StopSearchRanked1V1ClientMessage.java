package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ClientMessageType;

public class StopSearchRanked1V1ClientMessage extends MatchMakerClientMessage {

  public StopSearchRanked1V1ClientMessage() {
    super(ClientMessageType.GAME_MATCH_MAKING);
    state = "stop";
    mod = "ladder1v1";
  }
}
