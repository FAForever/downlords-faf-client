package com.faforever.client.relay;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * A local relay server to which Forged Alliance can connect to. All GPG commands received from FA are forwarded to the
 * FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnPacketFromOutsideListener(Consumer<DatagramPacket> listener);

  void addOnConnectionAcceptedListener(Runnable listener);

  Integer getGpgRelayPort();

  Integer getPublicPort();

  InetSocketAddress getRelayAddress();

  void removeOnPackedFromOutsideListener(Consumer<DatagramPacket> listener);

}
