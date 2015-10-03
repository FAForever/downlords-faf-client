package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ModInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockModsServerAccessor implements ModsServerAccessor {

  @Override
  public void connect() {

  }

  @Override
  public void disconnect() {

  }

  @Override
  public CompletableFuture<List<ModInfo>> searchMod(String name) {
    return CompletableFuture.completedFuture(null);
  }
}
