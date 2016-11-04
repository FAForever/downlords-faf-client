package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.domain.ClientMessageType;

public class SearchRanked1V1ClientMessage extends MatchMakerClientMessage {

  private Faction faction;

  public SearchRanked1V1ClientMessage(Faction faction) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = KnownFeaturedMod.LADDER_1V1.getString();
    state = "start";
    this.faction = faction;
  }

  public Faction getFaction() {
    return faction;
  }
}
