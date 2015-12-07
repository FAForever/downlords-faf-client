package com.faforever.client.connectivity;

import java.util.concurrent.CompletableFuture;

public interface ConnectivityService {

  ConnectivityState getConnectivityState();

  CompletableFuture<ConnectivityState> checkGamePortInBackground();
}
