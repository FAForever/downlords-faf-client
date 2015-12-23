package com.faforever.client.connectivity;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public interface TurnServerAccessor extends Closeable {

  void connect();

  void close() throws IOException;

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

  InetSocketAddress getMappedAddress();
}
