package com.faforever.client.relay;

import java.net.DatagramPacket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A local relay server to which Forged Alliance can connect to. All GPG commands received from FA are forwarded to the
 * FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnConnectionAcceptedListener(Runnable listener);

  Integer getGpgRelayPort();

  void removeOnPackedFromOutsideListener(Consumer<DatagramPacket> listener);

  /**
   * Starts the local relay server in background and completes the returned future with the opened TCP port when
   * started.
   *
   * @param packageReceiver a consumer that forwards packages sent by the game
   */
  CompletableFuture<Integer> start(Consumer<DatagramPacket> outgoingPackageConsumer, PackageReceiver packageReceiver);
}
