package com.faforever.client.parsecom;

import java.util.concurrent.CompletableFuture;

public class MockCloudService implements CloudService {

  @Override
  public CompletableFuture<String> signUpOrLogIn(String username, String password, String email, int uid) {
    return CompletableFuture.completedFuture(null);
  }
}
