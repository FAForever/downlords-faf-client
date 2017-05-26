package com.faforever.client.connectivity;

import java.net.DatagramPacket;
import java.util.function.Consumer;

/**
 * Sends and receives {@link DatagramPacket}s.
 */
public interface DatagramGateway {

  /**
   * Adds a listener to be called for any received package.
   */
  void addOnPacketListener(Consumer<DatagramPacket> listener);

  /**
   * Sends the specified packet through the gateway.
   */
  void send(DatagramPacket datagramPacket);

  void removeOnPacketListener(Consumer<DatagramPacket> listener);
}
