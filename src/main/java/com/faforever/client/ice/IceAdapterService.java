package com.faforever.client.ice;

import java.util.concurrent.CompletableFuture;

public interface IceAdapterService {
  CompletableFuture<IceAdapterClient> start();
}
