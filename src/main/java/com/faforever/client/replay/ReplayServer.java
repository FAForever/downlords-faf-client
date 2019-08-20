package com.faforever.client.replay;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.faforever.client.game.Game;

public interface ReplayServer {

  void stop();

  CompletableFuture<Integer> start(int gameId, Supplier<Game> gameInfoSupplier);
}
