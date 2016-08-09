package com.faforever.client.relay;

import com.faforever.client.connectivity.DatagramGateway;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;

/**
 * A local relay server to which Forged Alliance can connect to. All GPG commands received from FA are forwarded to the
 * FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  /**
   * Add a listener to be called whenever the game connected to this relay server.
   */
  void addOnGameConnectedListener(Runnable listener);

  /**
   * Returns the port number the server socket is listening on.
   */
  Integer getPort();

  /**
   * Starts the local relay server in background and completes the returned future with the opened TCP port when
   * started.
   *
   * @param gateway the {@link DatagramGateway} to use for incoming/outgoing datagram packets
   */
  CompletionStage<Integer> start(DatagramGateway gateway);

  /**
   * Returns the datagram socket address (IP/port) on which the game accepts packages.
   */
  InetSocketAddress getGameSocketAddress();

  void close();
}
