package com.faforever.client.leaderboard;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LeaderboardService {

  CompletableFuture<List<LeaderboardEntryBean>> getLadderInfo();

}
