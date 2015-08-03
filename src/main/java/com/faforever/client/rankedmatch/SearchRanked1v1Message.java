package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.domain.ClientMessageType;

public class SearchRanked1v1Message extends MatchMakerMessage {

  public int gameport;
  public String faction;

  public SearchRanked1v1Message(int gamePort, Faction faction) {
    command = ClientMessageType.GAME_MATCH_MAKING.getString();
    mod = FeaturedMod.LADDER_1V1.getString();
    state = "start";
    gameport = gamePort;

    // TODO this is BAD since there is a FactionSerializer, but it's a different way.
    switch (faction) {
      case UEF:
        this.faction = "/uef";
        break;
      case AEON:
        this.faction = "/aeon";
        break;
      case CYBRAN:
        this.faction = "/cybran";
        break;
      case SERAPHIM:
        this.faction = "/seraphim";
        break;
    }
  }
}
