package com.faforever.client.connectivity;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public interface TurnClient extends Closeable {

  /**
   * Connects to the TURN server and returns the allocated relay address.
   */
  void connect();

  void close() throws IOException;

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

  InetSocketAddress getMappedAddress();
}
