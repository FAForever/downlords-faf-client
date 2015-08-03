package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientObjectType;

public class Accept1v1Match extends ClientMessage {

  public final String mod;
  public final String state;
  public final Faction factionchosen;

  public Accept1v1Match(Faction faction) {
    command = ClientObjectType.ACCEPT_1V1_MATCH.getString();
    mod = "matchmaker";
    state = "faction";
    factionchosen = faction;
  }
}
