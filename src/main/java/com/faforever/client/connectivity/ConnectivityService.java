package com.faforever.client.connectivity;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public interface ConnectivityService {

  ConnectivityState getConnectivityState();

  CompletableFuture<ConnectivityState> checkGamePortInBackground();

  CompletableFuture<SocketAddress> ensureReachability();

  void disconnect();
}
