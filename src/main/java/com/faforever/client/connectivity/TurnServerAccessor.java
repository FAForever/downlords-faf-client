package com.faforever.client.connectivity;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public interface TurnServerAccessor {

  void ensureConnected();

  void disconnect();

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

  void setOnDataListener(Consumer<DatagramPacket> listener);
}
