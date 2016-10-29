package com.faforever.client.replay;

import java.util.concurrent.CompletableFuture;

public interface ReplayServer {

  void stop();

  CompletableFuture<Void> start(int uid);
}
