package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessageType;

public class Accept1v1MatchMessage extends MatchMakerMessage {

  private final Faction factionchosen;

  public Accept1v1MatchMessage(Faction faction) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = "matchmaker";
    state = "faction";
    factionchosen = faction;
  }

  public Faction getFactionchosen() {
    return factionchosen;
  }
}
