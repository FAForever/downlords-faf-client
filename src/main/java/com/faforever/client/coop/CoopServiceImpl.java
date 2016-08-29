package com.faforever.client.coop;

import com.faforever.client.api.CoopLeaderboardEntry;
import com.faforever.client.remote.FafService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CoopServiceImpl implements CoopService {

  @Resource
  FafService fafService;

  @Override
  public CompletableFuture<List<CoopMission>> getMissions() {
    return fafService.getCoopMaps();
  }

  @Override
  public CompletableFuture<List<CoopLeaderboardEntry>> getLeaderboard(CoopMission mission, int numberOfPlayers) {
    return fafService.getCoopLeaderboard(mission, numberOfPlayers);
  }
}
