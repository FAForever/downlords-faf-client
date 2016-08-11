package com.faforever.client.update;

import com.faforever.client.patch.GameUpdateService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockGameUpdateService implements GameUpdateService {

  @Override
  public CompletionStage<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> checkForUpdateInBackground() {
    return CompletableFuture.completedFuture(null);
  }
}
