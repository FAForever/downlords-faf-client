package com.faforever.client.coop;

import com.faforever.client.api.CoopLeaderboardEntry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CoopService {
  CompletableFuture<List<CoopMission>> getMissions();

  CompletableFuture<List<CoopLeaderboardEntry>> getLeaderboard(CoopMission mission, int numberOfPlayers);
}
