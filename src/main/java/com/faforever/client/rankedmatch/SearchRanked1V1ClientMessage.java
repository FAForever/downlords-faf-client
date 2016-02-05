package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameType;
import com.faforever.client.legacy.domain.ClientMessageType;

import java.net.SocketAddress;

public class SearchRanked1V1ClientMessage extends MatchMakerClientMessage {

  private Faction faction;
  private int gameport;
  private SocketAddress relayAddress;

  public SearchRanked1V1ClientMessage(int gamePort, Faction faction, SocketAddress relayAddress) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = GameType.LADDER_1V1.getString();
    state = "start";
    this.faction = faction;
    setGameport(gamePort);
    this.relayAddress = relayAddress;
  }

  public SocketAddress getRelayAddress() {
    return relayAddress;
  }

  public void setRelayAddress(SocketAddress relayAddress) {
    this.relayAddress = relayAddress;
  }

  public Faction getFaction() {
    return faction;
  }

  public void setFaction(Faction faction) {
    this.faction = faction;
  }

  public int getGameport() {
    return gameport;
  }

  public void setGameport(int gameport) {
    this.gameport = gameport;
  }
}
