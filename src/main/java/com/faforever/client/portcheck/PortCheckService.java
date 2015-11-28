package com.faforever.client.portcheck;

import java.util.concurrent.CompletableFuture;

public interface PortCheckService {

  CompletableFuture<ConnectivityState> checkGamePortInBackground();

}
