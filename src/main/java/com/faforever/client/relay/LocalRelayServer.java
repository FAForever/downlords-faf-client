package com.faforever.client.relay;

import java.util.concurrent.CompletableFuture;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnConnectionAcceptedListener(Runnable listener);

  CompletableFuture<Integer> startInBackground();

  void close();

}
