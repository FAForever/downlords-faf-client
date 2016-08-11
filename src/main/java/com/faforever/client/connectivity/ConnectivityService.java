package com.faforever.client.connectivity;

import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;

public interface ConnectivityService extends DatagramGateway {

  CompletionStage<Void> checkConnectivity();

  ReadOnlyObjectProperty<ConnectivityState> connectivityStateProperty();

  ConnectivityState getConnectivityState();

  InetSocketAddress getExternalSocketAddress();

  void connect();

  InetSocketAddress getRelayAddress();
}
