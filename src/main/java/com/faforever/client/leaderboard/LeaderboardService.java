package com.faforever.client.leaderboard;

import com.faforever.client.util.Tuple;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LeaderboardService {
  int MINIMUM_GAMES_PLAYED_TO_BE_SHOWN = 10;

  CompletableFuture<List<Leaderboard>> getLeaderboards();

  CompletableFuture<List<LeaderboardEntry>> getEntriesForPlayer(int playerId);

  CompletableFuture<List<LeaderboardEntry>> getEntries(Leaderboard leaderboard);

  CompletableFuture<Tuple<List<LeaderboardEntry>, Integer>> getPagedEntries(Leaderboard leaderboard, int count, int page);

  CompletableFuture<List<RatingStat>> getLeaderboardStats(String leaderboardTechnicalName);
}
