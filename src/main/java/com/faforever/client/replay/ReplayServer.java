package com.faforever.client.replay;

import java.util.concurrent.CompletableFuture;

public interface ReplayServer {

  void stop();

  CompletableFuture<Integer> start(int gameId);
}
