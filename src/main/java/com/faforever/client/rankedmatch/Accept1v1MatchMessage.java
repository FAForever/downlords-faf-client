package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;

public class Accept1v1MatchMessage extends MatchMakerMessage {

  public final Faction factionchosen;

  public Accept1v1MatchMessage(Faction faction) {
    mod = "matchmaker";
    state = "faction";
    factionchosen = faction;
  }
}
