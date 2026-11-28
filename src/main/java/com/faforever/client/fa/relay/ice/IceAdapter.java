package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Starts or stops the ICE adapter process.
 */
public interface IceAdapter {

  /** Called with a game's launch message, before {@link #start}. */
  default void prepare(GameLaunchResponse gameLaunchResponse) {
    // Only a network chosen per game needs it.
  }

  /** What the game's command line needs for this network. */
  default List<String> gameArguments() {
    return List.of();
  }

  void onIceAdapterStateChanged(String newState);

  void onGpgGameMessage(GpgGameOutboundMessage message);

  CompletableFuture<Integer> start(int gameId, boolean forceRelay);

  void stop();

  void setIceServers(Collection<CoturnServer> coturnServers);

  /** The lobby server's HostGame: the game hosts on that map once its lobby is up. */
  void hostGame(String mapName);

  /** The lobby server's JoinGame: the game joins the remote player's game. */
  void joinGame(String remotePlayerLogin, int remotePlayerId);

  /** The lobby server's ConnectToPeer: the game connects to that player, offering the connection or answering it. */
  void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer);

  /** The lobby server's DisconnectFromPeer. */
  void disconnectFromPeer(int remotePlayerId);

  /** The lobby server's IceMsg: an ICE message from that player for the connection to them. */
  void iceMsg(int remotePlayerId, Object message);
}
