package com.faforever.client.replay;

import com.faforever.client.api.FeaturedMod;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ReplayService {

  Collection<Replay> getLocalReplays();

  void runReplay(Replay item);

  void runLiveReplay(int gameId, int playerId);

  void runLiveReplay(URI uri);

  CompletableFuture<Void> startReplayServer(int gameUid);

  void stopReplayServer();

  void runReplay(Integer replayId);

  CompletableFuture<List<Replay>> searchByMap(String mapName);

  CompletableFuture<List<Replay>> searchByPlayer(String playerName);

  CompletableFuture<List<Replay>> searchByMod(FeaturedMod featuredMod);

  CompletionStage<List<Replay>> getNewestReplays(int topElementCount);

  CompletionStage<List<Replay>> getHighestRatedReplays(int topElementCount);

  CompletionStage<List<Replay>> getMostWatchedReplays(int topElementCount);
}
