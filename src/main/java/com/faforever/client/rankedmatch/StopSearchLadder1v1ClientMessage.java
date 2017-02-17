package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.ClientMessageType;

public class StopSearchLadder1v1ClientMessage extends MatchMakerClientMessage {

  public StopSearchLadder1v1ClientMessage() {
    super(ClientMessageType.GAME_MATCH_MAKING);
    state = "stop";
    mod = "ladder1v1";
  }
}
