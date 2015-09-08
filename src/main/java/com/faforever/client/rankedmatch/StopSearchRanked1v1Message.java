package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ClientMessageType;

public class StopSearchRanked1v1Message extends MatchMakerMessage {

  public StopSearchRanked1v1Message() {
    super(ClientMessageType.GAME_MATCH_MAKING);
    state = "stop";
    mod = "ladder1v1";
  }
}
