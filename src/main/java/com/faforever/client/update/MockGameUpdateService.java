package com.faforever.client.update;

import com.faforever.client.patch.GameUpdateService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MockGameUpdateService implements GameUpdateService {

  @Override
  public CompletableFuture<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> checkForUpdateInBackground() {
    return CompletableFuture.completedFuture(null);
  }
}
