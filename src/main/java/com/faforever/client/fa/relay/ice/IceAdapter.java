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

  /**
   * Whether the game running now could come back to its match if it crashed: only the game's own network can bring it
   * back, and only when the game said every player's engine can do it.
   */
  default boolean canRejoin() {
    return false;
  }

  /**
   * The arguments that start the game so that it takes its seat back in its match. Empty when this network can't.
   */
  default List<String> rejoinArguments() {
    return List.of();
  }
}
