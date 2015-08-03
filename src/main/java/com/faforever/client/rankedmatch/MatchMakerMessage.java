package com.faforever.client.rankedmatch;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;

public class MatchMakerMessage extends ClientMessage {

  public String mod;
  public String state;

  public MatchMakerMessage() {
    command = ClientMessageType.GAME_MATCH_MAKING.getString();
  }
}
