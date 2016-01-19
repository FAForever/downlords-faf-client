package com.faforever.client.connectivity;

import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ConnectivityService {

  CompletableFuture<Void> checkConnectivity();

  ReadOnlyObjectProperty<ConnectivityState> connectivityStateProperty();

  ConnectivityState getConnectivityState();

  InetSocketAddress getExternalSocketAddress();

  Consumer<DatagramPacket> ensureConnection();
}
