package com.faforever.client.connectivity;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public interface TurnServerAccessor extends Closeable {

  void connect();

  void close();

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

  InetSocketAddress getMappedAddress();

  void setOnDataListener(Consumer<DatagramPacket> listener);

  /**
   * Removes all peer bindings.
   */
  void unbind();
}
