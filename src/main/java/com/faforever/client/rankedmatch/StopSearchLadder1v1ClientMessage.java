package com.faforever.client.rankedmatch;

import com.faforever.client.fa.RatingMode;
import com.faforever.client.remote.domain.ClientMessageType;

public class StopSearchLadder1v1ClientMessage extends MatchMakerClientMessage {

  public StopSearchLadder1v1ClientMessage(RatingMode ratingMode) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    state = "stop";
    mod = ratingMode.getCorrespondingFeatureMod().getTechnicalName();
  }
}
