package com.faforever.client.ice;

import java.util.concurrent.CompletableFuture;

public interface IceAdapter {
  CompletableFuture<Void> start();

  void stop();
}
