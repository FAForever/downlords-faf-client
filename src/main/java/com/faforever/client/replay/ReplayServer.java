package com.faforever.client.replay;

import com.faforever.client.domain.GameBean;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ReplayServer {

  void stop();

  CompletableFuture<Integer> start(int gameId, Supplier<GameBean> onGameInfoFinished);
}
