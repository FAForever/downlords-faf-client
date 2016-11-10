package com.faforever.client.fa.relay.ice;

import java.util.concurrent.CompletableFuture;

/**
 * Controls the ICE adapter binary.
 */
public interface IceAdapter {
  CompletableFuture<Integer> start();

  void stop();
}
