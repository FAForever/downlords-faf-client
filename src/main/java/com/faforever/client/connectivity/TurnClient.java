package com.faforever.client.connectivity;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public interface TurnClient extends Closeable {

  CompletableFuture<SocketAddress> connect();

  void close() throws IOException;

  InetSocketAddress getRelayAddress();

  void send(DatagramPacket datagramPacket);

}
