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
public class CoopServiceImpl implements CoopService {

  private final FafService fafService;

  @Inject
  public CoopServiceImpl(FafService fafService) {
    this.fafService = fafService;
  }

  @Override
  public CompletableFuture<List<CoopMission>> getMissions() {
    return fafService.getCoopMaps();
  }

  @Override
  public CompletableFuture<List<CoopResult>> getLeaderboard(CoopMission mission, int numberOfPlayers) {
    return fafService.getCoopLeaderboard(mission, numberOfPlayers);
  }
}
