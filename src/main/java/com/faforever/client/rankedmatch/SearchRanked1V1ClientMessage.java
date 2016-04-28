package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameType;
import com.faforever.client.remote.domain.ClientMessageType;

import java.net.InetSocketAddress;

public class SearchRanked1V1ClientMessage extends MatchMakerClientMessage {

  private Faction faction;
  private int gameport;
  private InetSocketAddress relayAddress;

  public SearchRanked1V1ClientMessage(int gamePort, Faction faction, InetSocketAddress relayAddress) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    this.relayAddress = relayAddress;
    mod = GameType.LADDER_1V1.getString();
    state = "start";
    this.faction = faction;
    setGameport(gamePort);
  }

  public Faction getFaction() {
    return faction;
  }

  public int getGameport() {
    return gameport;
  }

  public void setGameport(int gameport) {
    this.gameport = gameport;
  }

  public InetSocketAddress getRelayAddress() {
    return relayAddress;
  }

  public void setRelayAddress(InetSocketAddress relayAddress) {
    this.relayAddress = relayAddress;
  }
}
