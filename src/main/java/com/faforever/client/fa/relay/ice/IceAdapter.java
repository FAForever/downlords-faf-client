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
  default void prepare(GameLaunchResponse gameLaunchResponse) {}

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
   * Whether the game running now could come back to its match if it crashed: the preference is on, and the game said
   * every player's engine can do it.
   */
  boolean canRejoin();

  /**
   * The arguments that start the game so that it takes its seat back, through this adapter's links to the other
   * players. Empty when the adapter no longer has them.
   */
  List<String> rejoinArguments();
}
