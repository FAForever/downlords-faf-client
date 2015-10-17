package com.faforever.client.parsecom;

import java.util.concurrent.CompletableFuture;

public class MockCloudAccessor implements CloudAccessor {

  @Override
  public CompletableFuture<String> signUpOrLogIn(String username, String password, int uid) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<String> getPlayerIdForUsername(String username) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<String> setPlayerId(String playerId) {
    return CompletableFuture.completedFuture(null);
  }
}
