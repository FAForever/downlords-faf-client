package com.faforever.client.replay;

import com.faforever.client.api.dto.FeaturedMod;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

  CompletableFuture<List<Replay>> getNewestReplays(int topElementCount);

  CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount);

  CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount);
}
