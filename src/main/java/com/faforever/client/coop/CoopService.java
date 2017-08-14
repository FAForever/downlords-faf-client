package com.faforever.client.coop;

import com.faforever.client.api.dto.CoopResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CoopService {
  CompletableFuture<List<CoopMission>> getMissions();

  CompletableFuture<List<CoopResult>> getLeaderboard(CoopMission mission, int numberOfPlayers);
}
