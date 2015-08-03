package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.FeaturedMod;
import com.faforever.client.legacy.domain.ClientMessageType;

public class SearchRanked1v1Message extends MatchMakerMessage {

  public int gameport;
  public Faction faction;

  public SearchRanked1v1Message(int gamePort, Faction faction) {
    command = ClientMessageType.GAME_MATCH_MAKING.getString();
    mod = FeaturedMod.LADDER_1V1.getString();
    state = "start";
    gameport = gamePort;
    this.faction = faction;
  }
}
