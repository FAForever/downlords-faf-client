package com.faforever.client.connectivity;

import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public interface ConnectivityService {

  CompletableFuture<Void> checkConnectivity();

  CompletableFuture<InetSocketAddress> setUpConnection();

  void closeRelayConnection();

  void sendGameData(DatagramPacket datagramPacket);

  ReadOnlyObjectProperty<ConnectivityState> connectivityStateProperty();

  ConnectivityState getConnectivityState();

  int getExternalPort();
}
