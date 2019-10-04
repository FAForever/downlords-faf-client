package com.faforever.client.coop;

import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.remote.FafService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Lazy
@Service
public class CoopService {

  private final FafService fafService;


  public CoopService(FafService fafService) {
    this.fafService = fafService;
  }

  public CompletableFuture<List<CoopMission>> getMissions() {
    return fafService.getCoopMaps();
  }

  public CompletableFuture<List<CoopResult>> getLeaderboard(CoopMission mission, int numberOfPlayers) {
    return fafService.getCoopLeaderboard(mission, numberOfPlayers);
  }
}
