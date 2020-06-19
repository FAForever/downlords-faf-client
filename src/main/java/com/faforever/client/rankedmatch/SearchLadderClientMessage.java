package com.faforever.client.rankedmatch;

import com.faforever.client.fa.RatingMode;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.domain.ClientMessageType;

public class SearchLadderClientMessage extends MatchMakerClientMessage {

  private Faction faction;

  public SearchLadderClientMessage(RatingMode ratingMode, Faction faction) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = ratingMode.getCorrespondingFeatureMod().getTechnicalName();
    state = "start";
    this.faction = faction;
  }

  public Faction getFaction() {
    return faction;
  }
}
