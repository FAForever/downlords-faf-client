package com.faforever.client.relay;

import com.faforever.client.connectivity.DatagramGateway;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * A local relay server to which Forged Alliance can connect to. All GPG commands received from FA are forwarded to the
 * FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {


  void addOnConnectionAcceptedListener(Runnable listener);

  Integer getPort();

  /**
   * Starts the local relay server in background and completes the returned future with the opened TCP port when
   * started.
   *
   * @param gateway the {@link DatagramGateway} to use for incoming/outgoing datagram packets
   */
  CompletableFuture<Integer> start(DatagramGateway gateway);

  InetSocketAddress getGameSocketAddress();

  void close();
}
