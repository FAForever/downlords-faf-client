package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Starts or stops the ICE adapter process.
 */
public interface IceAdapter {
  CompletableFuture<Integer> start(int gameId);

  void stop();

  void setIceServers(Collection<CoturnServer> coturnServers);
}
