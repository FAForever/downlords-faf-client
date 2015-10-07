package com.faforever.client.parsecom;

import java.util.concurrent.CompletableFuture;

public interface CloudService {

  CompletableFuture<String> signUpOrLogIn(String username, String password);
}
