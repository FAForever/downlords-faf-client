package com.faforever.client.replay;

import java.net.URI;
import java.nio.file.Path;
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

  CompletableFuture<List<Replay>> getNewestReplays(int topElementCount);

  CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount);

  CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount);

  CompletableFuture<List<Replay>> findByQuery(String condition, int maxResults);

  CompletableFuture<Path> downloadReplay(int id);

  /**
   * Reads the specified replay file in order to add more information to the specified replay instance.
   */
  void enrich(Replay replay, Path path);

  CompletableFuture<Integer> getSize(int id);
}
