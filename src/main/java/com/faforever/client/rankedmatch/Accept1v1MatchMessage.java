package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessageType;

public class Accept1v1MatchMessage extends MatchMakerMessage {

  public final Faction factionchosen;

  public Accept1v1MatchMessage(Faction faction) {
    command = ClientMessageType.GAME_MATCH_MAKING.getString();
    mod = "matchmaker";
    state = "faction";
    factionchosen = faction;
  }
}
