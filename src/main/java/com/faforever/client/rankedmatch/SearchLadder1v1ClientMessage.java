package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.domain.ClientMessageType;

public class SearchLadder1v1ClientMessage extends MatchMakerClientMessage {

  private Faction faction;

  public SearchLadder1v1ClientMessage(Faction faction) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = KnownFeaturedMod.LADDER_1V1.getTechnicalName();
    state = "start";
    this.faction = faction;
  }

  public Faction getFaction() {
    return faction;
  }
}
