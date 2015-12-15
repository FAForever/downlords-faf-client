package com.faforever.client.relay;

import java.util.concurrent.CompletableFuture;

/**
 * A local relay server to which Forged Alliance can connect to. All GPG commands received from FA are forwarded to the
 * FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnConnectionAcceptedListener(Runnable listener);

  /**
   * Starts the relay server asynchronous.
   *
   * @return the port that has been opened
   */
  CompletableFuture<Integer> startAsync();

  void getPort();
}
