package com.faforever.client.rankedmatch;

import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.remote.domain.ClientMessageType;
import com.google.gson.annotations.SerializedName;

import java.net.InetSocketAddress;

public class SearchLadder1v1ClientMessage extends MatchMakerClientMessage {

  private Faction faction;
  private int gameport;
  @SerializedName("relay_address")
  private InetSocketAddress relayAddress;

  public SearchLadder1v1ClientMessage(int gamePort, Faction faction, InetSocketAddress relayAddress) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    mod = KnownFeaturedMod.LADDER_1V1.getTechnicalName();
    state = "start";
    this.faction = faction;
    setGameport(gamePort);
    setRelayAddress(relayAddress);
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
