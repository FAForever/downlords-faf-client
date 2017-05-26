package com.faforever.client.connectivity;

import com.faforever.client.net.ConnectionState;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public interface TurnServerAccessor extends DatagramGateway {

  void disconnect();

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

  ConnectionState getConnectionState();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  void connect();

  /**
   * Returns {@code true} if the specified address is bound to a channel.
   */
  boolean isBound(InetSocketAddress socketAddress);

  void bind(InetSocketAddress socketAddress);
}
