package com.faforever.client.parsecom;

import java.util.concurrent.CompletableFuture;

public interface CloudAccessor {

  CompletableFuture<String> signUpOrLogIn(String username, String password, int uid);

  CompletableFuture<String> getPlayerIdForUsername(String username);

  CompletableFuture<String> setPlayerId(String playerId);
}
