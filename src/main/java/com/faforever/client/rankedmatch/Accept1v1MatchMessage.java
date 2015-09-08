package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessageType;

public class Accept1v1MatchMessage extends MatchMakerMessage {

  private final Faction faction;
  private final int gameport;

  public Accept1v1MatchMessage(Faction faction, int port) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = "ladder1v1";
    state = "start";
    this.gameport = 6112;
    this.faction = faction;
  }

  public Faction getFaction() {
    return faction;
  }

  public int getGameport() {
    return gameport;
  }
}
