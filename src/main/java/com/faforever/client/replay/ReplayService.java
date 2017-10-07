package com.faforever.client.replay;


import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ReplayService {

  Collection<Replay> getLocalReplays();

  void runReplay(Replay item);

  void runLiveReplay(int gameId, int playerId);

  void runLiveReplay(URI uri);

  CompletableFuture<Void> startReplayServer(int gameUid);

  void stopReplayServer();

  void runReplay(Integer replayId);

  CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page);

  CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount, int page);

  CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount, int page);

  CompletableFuture<List<Replay>> findByQuery(String condition, int maxResults, int page);

  CompletableFuture<Optional<Replay>> findById(int id);

  CompletableFuture<Path> downloadReplay(int id);

  /**
   * Reads the specified replay file in order to add more information to the specified replay instance.
   */
  void enrich(Replay replay, Path path);

  CompletableFuture<Integer> getSize(int id);
}
