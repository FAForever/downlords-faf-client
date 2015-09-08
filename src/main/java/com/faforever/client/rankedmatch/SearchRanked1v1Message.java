package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.domain.ClientMessageType;

public class SearchRanked1v1Message extends MatchMakerMessage {

  public String faction;
  private int gameport;

  public SearchRanked1v1Message(int gamePort, Faction faction) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = FeaturedMod.LADDER_1V1.getString();
    state = "start";
    setGameport(gamePort);

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

  public String getFaction() {
    return faction;
  }

  public int getGameport() {
    return gameport;
  }

  public void setGameport(int gameport) {
    this.gameport = gameport;
  }
}
